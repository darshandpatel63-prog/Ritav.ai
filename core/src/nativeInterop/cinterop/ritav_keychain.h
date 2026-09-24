#ifndef RITAV_KEYCHAIN_H
#define RITAV_KEYCHAIN_H

#include <CoreFoundation/CoreFoundation.h>
#include <Security/Security.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>

#define RITAV_KEYCHAIN_NOT_FOUND (-1)
#define RITAV_KEYCHAIN_ERROR (-2)
#define RITAV_KEYCHAIN_TOO_LARGE (-3)

static CFStringRef ritav_cf_string(const char *value) {
    if (value == NULL) return NULL;
    return CFStringCreateWithCString(kCFAllocatorDefault, value, kCFStringEncodingUTF8);
}

static CFDictionaryRef ritav_query(const char *service, const char *account) {
    CFStringRef service_ref = ritav_cf_string(service);
    CFStringRef account_ref = ritav_cf_string(account);
    if (service_ref == NULL || account_ref == NULL) {
        if (service_ref != NULL) CFRelease(service_ref);
        if (account_ref != NULL) CFRelease(account_ref);
        return NULL;
    }

    const void *keys[] = { kSecClass, kSecAttrService, kSecAttrAccount };
    const void *values[] = { kSecClassGenericPassword, service_ref, account_ref };
    CFDictionaryRef query = CFDictionaryCreate(
        kCFAllocatorDefault,
        keys,
        values,
        3,
        &kCFTypeDictionaryKeyCallBacks,
        &kCFTypeDictionaryValueCallBacks
    );

    CFRelease(service_ref);
    CFRelease(account_ref);
    return query;
}

static int ritav_keychain_put(
    const char *service,
    const char *account,
    const uint8_t *data,
    size_t length
) {
    if (data == NULL && length != 0) return RITAV_KEYCHAIN_ERROR;

    CFStringRef service_ref = ritav_cf_string(service);
    CFStringRef account_ref = ritav_cf_string(account);
    CFDataRef data_ref = CFDataCreate(
        kCFAllocatorDefault,
        data,
        (CFIndex) length
    );
    if (service_ref == NULL || account_ref == NULL || data_ref == NULL) {
        if (service_ref != NULL) CFRelease(service_ref);
        if (account_ref != NULL) CFRelease(account_ref);
        if (data_ref != NULL) CFRelease(data_ref);
        return RITAV_KEYCHAIN_ERROR;
    }

    const void *delete_keys[] = { kSecClass, kSecAttrService, kSecAttrAccount };
    const void *delete_values[] = { kSecClassGenericPassword, service_ref, account_ref };
    CFDictionaryRef delete_query = CFDictionaryCreate(
        kCFAllocatorDefault,
        delete_keys,
        delete_values,
        3,
        &kCFTypeDictionaryKeyCallBacks,
        &kCFTypeDictionaryValueCallBacks
    );
    if (delete_query != NULL) {
        SecItemDelete(delete_query);
        CFRelease(delete_query);
    }

    const void *keys[] = {
        kSecClass,
        kSecAttrService,
        kSecAttrAccount,
        kSecValueData,
        kSecAttrAccessible
    };
    const void *values[] = {
        kSecClassGenericPassword,
        service_ref,
        account_ref,
        data_ref,
        kSecAttrAccessibleWhenUnlockedThisDeviceOnly
    };
    CFDictionaryRef add_query = CFDictionaryCreate(
        kCFAllocatorDefault,
        keys,
        values,
        5,
        &kCFTypeDictionaryKeyCallBacks,
        &kCFTypeDictionaryValueCallBacks
    );

    OSStatus status = add_query == NULL ? errSecParam : SecItemAdd(add_query, NULL);

    if (add_query != NULL) CFRelease(add_query);
    CFRelease(service_ref);
    CFRelease(account_ref);
    CFRelease(data_ref);

    return status == errSecSuccess ? 0 : RITAV_KEYCHAIN_ERROR;
}

static int ritav_keychain_get(
    const char *service,
    const char *account,
    uint8_t *out_buffer,
    size_t capacity
) {
    if (out_buffer == NULL && capacity != 0) return RITAV_KEYCHAIN_ERROR;

    CFStringRef service_ref = ritav_cf_string(service);
    CFStringRef account_ref = ritav_cf_string(account);
    if (service_ref == NULL || account_ref == NULL) {
        if (service_ref != NULL) CFRelease(service_ref);
        if (account_ref != NULL) CFRelease(account_ref);
        return RITAV_KEYCHAIN_ERROR;
    }

    const void *keys[] = {
        kSecClass,
        kSecAttrService,
        kSecAttrAccount,
        kSecReturnData,
        kSecMatchLimit
    };
    const void *values[] = {
        kSecClassGenericPassword,
        service_ref,
        account_ref,
        kCFBooleanTrue,
        kSecMatchLimitOne
    };
    CFDictionaryRef query = CFDictionaryCreate(
        kCFAllocatorDefault,
        keys,
        values,
        5,
        &kCFTypeDictionaryKeyCallBacks,
        &kCFTypeDictionaryValueCallBacks
    );

    CFTypeRef result = NULL;
    OSStatus status = query == NULL ? errSecParam : SecItemCopyMatching(query, &result);

    if (query != NULL) CFRelease(query);
    CFRelease(service_ref);
    CFRelease(account_ref);

    if (status == errSecItemNotFound) return RITAV_KEYCHAIN_NOT_FOUND;
    if (status != errSecSuccess || result == NULL) {
        if (result != NULL) CFRelease(result);
        return RITAV_KEYCHAIN_ERROR;
    }

    if (CFGetTypeID(result) != CFDataGetTypeID()) {
        CFRelease(result);
        return RITAV_KEYCHAIN_ERROR;
    }

    CFIndex length = CFDataGetLength((CFDataRef) result);
    if (length < 0 || (size_t) length > capacity) {
        CFRelease(result);
        return RITAV_KEYCHAIN_TOO_LARGE;
    }

    if (length > 0) {
        memcpy(out_buffer, CFDataGetBytePtr((CFDataRef) result), (size_t) length);
    }
    CFRelease(result);
    return (int) length;
}

static int ritav_keychain_delete(
    const char *service,
    const char *account
) {
    CFDictionaryRef query = ritav_query(service, account);
    if (query == NULL) return RITAV_KEYCHAIN_ERROR;

    OSStatus status = SecItemDelete(query);
    CFRelease(query);

    if (status == errSecSuccess) return 0;
    if (status == errSecItemNotFound) return RITAV_KEYCHAIN_NOT_FOUND;
    return RITAV_KEYCHAIN_ERROR;
}

#endif
