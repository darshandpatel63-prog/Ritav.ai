package ai.ritav.app.core.security

import java.text.Normalizer

enum class SensitiveDataType { OTP, UPI_PIN, PASSWORD, CVV, RECOVERY_CODE, PRIVATE_KEY, API_KEY, UNKNOWN_SECRET }
enum class FirewallBlockReason { SENSITIVE_DATA_DETECTED, INPUT_TOO_LARGE, NORMALIZATION_INSPECTION_FAILED }
data class SensitiveMatch(val type: SensitiveDataType, val start: Int, val end: Int)
data class FirewallResult(val allowed: Boolean, val redactedText: String, val matches: List<SensitiveMatch>, val blockReason: FirewallBlockReason? = null)

class SensitiveInformationFirewall {
    fun inspect(text: String): FirewallResult {
        if (text.length > MAX_INPUT_LENGTH) return FirewallResult(false, "", emptyList(), FirewallBlockReason.INPUT_TOO_LARGE)
        if (text.isEmpty()) return FirewallResult(true, text, emptyList())
        val matches = findDirectMatches(text)
        if (matches.isNotEmpty()) {
            var redacted = text
            matches.sortedByDescending { it.start }.forEach { match ->
                redacted = redacted.substring(0, match.start) + "[REDACTED:${match.type.name}]" + redacted.substring(match.end)
            }
            return FirewallResult(false, redacted, matches, FirewallBlockReason.SENSITIVE_DATA_DETECTED)
        }
        val transformed = runCatching {
            val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
            val compact = compactCodePoints(normalized)
            val punctuationCompacted = compactSensitiveLabels(compact)
            val confusableFolded = foldCommonLatinConfusables(punctuationCompacted)
            val digitFolded = foldUnicodeDecimalDigits(confusableFolded)
            TransformedRepresentations(normalized, compact, punctuationCompacted, confusableFolded, digitFolded)
        }.getOrElse { return FirewallResult(false, "", emptyList(), FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED) }
        val changed = transformed.normalized != text || transformed.compact != transformed.normalized ||
            transformed.punctuationCompacted != transformed.compact || transformed.confusableFolded != transformed.punctuationCompacted ||
            transformed.digitFolded != transformed.confusableFolded
        if (changed && containsSensitivePattern(transformed.normalized, transformed.compact, transformed.punctuationCompacted, transformed.confusableFolded, transformed.digitFolded))
            return FirewallResult(false, "", emptyList(), FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED)
        return FirewallResult(true, text, emptyList())
    }

    private data class TransformedRepresentations(val normalized: String, val compact: String, val punctuationCompacted: String, val confusableFolded: String, val digitFolded: String)
    private fun findDirectMatches(text: String): List<SensitiveMatch> = buildList {
        OTP.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.OTP, it.range.first, it.range.last + 1)) }
        UPI_PIN.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.UPI_PIN, it.range.first, it.range.last + 1)) }
        CVV.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.CVV, it.range.first, it.range.last + 1)) }
        PRIVATE_KEY.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.PRIVATE_KEY, it.range.first, it.range.last + 1)) }
        API_KEY.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.API_KEY, it.range.first, it.range.last + 1)) }
        STANDALONE_API_KEY.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.API_KEY, it.range.first, it.range.last + 1)) }
        RECOVERY_CODE.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.RECOVERY_CODE, it.range.first, it.range.last + 1)) }
        PASSWORD_CONTEXT.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.PASSWORD, it.range.first, it.range.last + 1)) }
    }.distinctBy { Triple(it.type, it.start, it.end) }.sortedWith(compareBy<SensitiveMatch> { it.start }.thenByDescending { it.end - it.start }).let(::removeOverlappingMatches)

    private fun containsSensitivePattern(vararg candidates: String): Boolean = candidates.any { candidate ->
        listOf(OTP, OTP_COMPACT, UPI_PIN, UPI_PIN_COMPACT, CVV, CVV_COMPACT, PRIVATE_KEY, API_KEY, API_KEY_COMPACT, STANDALONE_API_KEY, RECOVERY_CODE, PASSWORD_CONTEXT, PASSWORD_CONTEXT_COMPACT).any { it.containsMatchIn(candidate) }
    }
    private fun compactCodePoints(text: String): String = buildString(text.length) { var i = 0; while (i < text.length) { val cp = text.codePointAt(i); if (!Character.isWhitespace(cp) && Character.getType(cp) != Character.FORMAT.toInt()) appendCodePoint(cp); i += Character.charCount(cp) } }
    private fun compactSensitiveLabels(text: String): String = buildString(text.length) { var i = 0; while (i < text.length) { val cp = text.codePointAt(i); val type = Character.getType(cp); if (!isPunctuation(type) || cp == ':'.code || cp == '='.code) appendCodePoint(cp); i += Character.charCount(cp) } }
    private fun isPunctuation(type: Int): Boolean = type == Character.CONNECTOR_PUNCTUATION.toInt() || type == Character.DASH_PUNCTUATION.toInt() || type == Character.START_PUNCTUATION.toInt() || type == Character.END_PUNCTUATION.toInt() || type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() || type == Character.FINAL_QUOTE_PUNCTUATION.toInt() || type == Character.OTHER_PUNCTUATION.toInt()
    private fun foldCommonLatinConfusables(text: String): String = buildString(text.length) { text.forEach { c -> append(when (c) { '\u0391','\u0410'->'A'; '\u0392','\u0412'->'B'; '\u03A7','\u0425'->'X'; '\u0395','\u0415'->'E'; '\u0397','\u041D'->'H'; '\u0399','\u0406'->'I'; '\u039A','\u041A'->'K'; '\u039C','\u041C'->'M'; '\u039F','\u041E'->'O'; '\u03A1','\u0420'->'P'; '\u03A4','\u0422'->'T'; '\u03A5','\u04AE'->'Y'; '\u0396','\u0417'->'Z'; '\u03B1','\u0430'->'a'; '\u03B5','\u0435'->'e'; '\u03B7','\u043D'->'h'; '\u03B9','\u0456'->'i'; '\u03BA','\u043A'->'k'; '\u03BC','\u043C'->'m'; '\u03BF','\u043E'->'o'; '\u03C1','\u0440'->'p'; '\u03C4','\u0442'->'t'; '\u03C5','\u0443'->'y'; '\u03C7','\u0445'->'x'; '\u0437'->'z'; else->c }) } }
    private fun foldUnicodeDecimalDigits(text: String): String = buildString(text.length) { var i=0; while(i<text.length){val cp=text.codePointAt(i); if(Character.getType(cp)==Character.DECIMAL_DIGIT_NUMBER.toInt()){val d=Character.digit(cp,10); if(d>=0) append(('0'.code+d).toChar()) else appendCodePoint(cp)}else appendCodePoint(cp); i+=Character.charCount(cp)} }
    private fun removeOverlappingMatches(matches: List<SensitiveMatch>): List<SensitiveMatch> { val selected=mutableListOf<SensitiveMatch>(); for(candidate in matches) if(selected.none{candidate.start<it.end&&candidate.end>it.start}) selected+=candidate; return selected }
    companion object {
        private const val MAX_INPUT_LENGTH=16_384
        private val OTP=Regex("(?i)(?:otp|one[- ]?time\\s*password|verification code|security code)\\s*(?:is|:|=)?\\s*\\b\\d{4,8}\\b")
        private val OTP_COMPACT=Regex("(?i)(?:otp|onetimepassword|verificationcode|securitycode)(?:is|:|=)?\\s*\\d{4,8}(?!\\d)")
        private val UPI_PIN=Regex("(?i)(?:upi\\s*pin|pin for upi)\\s*(?:is|:|=)?\\s*\\b\\d{4,6}\\b")
        private val UPI_PIN_COMPACT=Regex("(?i)(?:upipin|pinforupi)(?:is|:|=)?\\s*\\d{4,6}(?!\\d)")
        private val CVV=Regex("(?i)(?:cvv|cvc|security code)\\s*(?:is|:|=)?\\s*\\b\\d{3,4}\\b")
        private val CVV_COMPACT=Regex("(?i)(?:cvv|cvc|securitycode)(?:is|:|=)?\\s*\\d{3,4}(?!\\d)")
        private val PRIVATE_KEY=Regex("-----BEGIN(?: [A-Z0-9][A-Z0-9 ]{0,63})? PRIVATE KEY-----[\\s\\S]*?-----END(?: [A-Z0-9][A-Z0-9 ]{0,63})? PRIVATE KEY-----")
        private val API_KEY=Regex("(?i)\\b(?:api[_ ]?key|access[_ ]?token|secret[_ ]?key)\\s*(?:is|:|=)\\s*[A-Za-z0-9_./+=-]{12,}")
        private val API_KEY_COMPACT=Regex("(?i)\\b(?:apikey|accesstoken|secretkey)(?:is|:|=)\\s*[A-Za-z0-9_./+=-]{12,}")
        private val STANDALONE_API_KEY=Regex("(?i)(?<![A-Za-z0-9_])(?:sk-[A-Za-z0-9]{20,}|sk-(?:[A-Za-z0-9]+-)+[A-Za-z0-9]{10,}|ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|xox[bp]-[A-Za-z0-9-]{20,}|AKIA[0-9A-Z]{18}|AIza[0-9A-Za-z_-]{30,})(?![A-Za-z0-9_])")
        private val RECOVERY_CODE=Regex("(?i)(?:recovery|backup|emergency)\\s*code(?:s)?\\s*(?:are|is|:|=)?\\s*\\b[A-Za-z0-9-]{6,32}(?:\\s*,\\s*[A-Za-z0-9-]{6,32})*\\b")
        private val PASSWORD_CONTEXT=Regex("(?i)(?:password|passcode|login\\s*password)\\s*(?:is|:|=)\\s*[^\\s,;]{4,}")
        private val PASSWORD_CONTEXT_COMPACT=Regex("(?i)(?:password|passcode|loginpassword)(?:is|:|=)\\s*[^\\s,;]{4,}")
    }
}
