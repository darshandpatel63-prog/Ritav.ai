#ifndef RITAV_LIBSECRET_H
#define RITAV_LIBSECRET_H

#include <glib.h>
#include <libsecret/secret.h>
#include <string.h>

#define RITAV_MAX_SECRET_VALUE_BYTES 131072

static inline GHashTable* ritav_secret_attributes(
    const char* service,
    const char* name
) {
    GHashTable* attributes = g_hash_table_new_full(
        g_str_hash,
        g_str_equal,
        g_free,
        g_free
    );
    if (attributes == NULL) {
        return NULL;
    }

    char* service_key = g_strdup("ritav-service");
    char* service_value = g_strdup(service);
    char* name_key = g_strdup("ritav-name");
    char* name_value = g_strdup(name);

    if (service_key == NULL || service_value == NULL ||
        name_key == NULL || name_value == NULL) {
        g_free(service_key);
        g_free(service_value);
        g_free(name_key);
        g_free(name_value);
        g_hash_table_destroy(attributes);
        return NULL;
    }

    g_hash_table_insert(attributes, service_key, service_value);
    g_hash_table_insert(attributes, name_key, name_value);
    return attributes;
}

/*
 * Returns:
 *   1 = stored
 *   0 = storage refused/failed without a GError
 *  -1 = libsecret/Secret Service error
 */
static inline int ritav_secret_store(
    const char* service,
    const char* name,
    const char* value
) {
    if (service == NULL || name == NULL || value == NULL) {
        return -1;
    }

    GHashTable* attributes = ritav_secret_attributes(service, name);
    if (attributes == NULL) {
        return -1;
    }

    GError* error = NULL;
    gboolean stored = secret_password_storev_sync(
        NULL,
        attributes,
        NULL,
        "Ritav.ai secure state",
        value,
        NULL,
        &error
    );

    g_hash_table_destroy(attributes);

    if (error != NULL) {
        g_error_free(error);
        return -1;
    }

    return stored ? 1 : 0;
}

/*
 * Returns:
 *   1 = found; out_value receives owned secret string
 *   0 = not found
 *  -1 = Secret Service error
 *  -2 = returned secret exceeds Ritav.ai read bound
 */
static inline int ritav_secret_lookup(
    const char* service,
    const char* name,
    char** out_value
) {
    if (service == NULL || name == NULL || out_value == NULL) {
        return -1;
    }
    *out_value = NULL;

    GHashTable* attributes = ritav_secret_attributes(service, name);
    if (attributes == NULL) {
        return -1;
    }

    GError* error = NULL;
    gchar* value = secret_password_lookupv_sync(
        NULL,
        attributes,
        NULL,
        &error
    );

    g_hash_table_destroy(attributes);

    if (error != NULL) {
        if (value != NULL) {
            secret_password_free(value);
        }
        g_error_free(error);
        return -1;
    }

    if (value == NULL) {
        return 0;
    }

    /*
     * Bound scanning before Kotlin decodes the returned C string. The loop
     * stops at the first NUL, so a shorter C string is never read past its
     * allocated terminator; an oversized string is rejected at the configured
     * bound before native-to-Kotlin decoding.
     */
    gsize bounded_length = 0;
    while (bounded_length <= RITAV_MAX_SECRET_VALUE_BYTES &&
           value[bounded_length] != '\0') {
        ++bounded_length;
    }
    if (bounded_length > RITAV_MAX_SECRET_VALUE_BYTES) {
        secret_password_free(value);
        return -2;
    }

    *out_value = value;
    return 1;
}

/*
 * Returns:
 *   1 = at least one matching secret removed
 *   0 = no matching secret
 *  -1 = Secret Service error
 */
static inline int ritav_secret_clear(
    const char* service,
    const char* name
) {
    if (service == NULL || name == NULL) {
        return -1;
    }

    GHashTable* attributes = ritav_secret_attributes(service, name);
    if (attributes == NULL) {
        return -1;
    }

    GError* error = NULL;
    gboolean removed = secret_password_clearv_sync(
        NULL,
        attributes,
        NULL,
        &error
    );

    g_hash_table_destroy(attributes);

    if (error != NULL) {
        g_error_free(error);
        return -1;
    }

    return removed ? 1 : 0;
}

static inline void ritav_secret_free(char* value) {
    if (value != NULL) {
        secret_password_free(value);
    }
}

#endif
