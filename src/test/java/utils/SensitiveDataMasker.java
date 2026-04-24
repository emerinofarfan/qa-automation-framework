package utils;

import io.qameta.allure.model.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Enmascara datos sensibles antes de que se publiquen en reportes.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Si el nombre del campo es sensible, reemplaza el valor por ***.</li>
 *   <li>Si el valor parece token/credencial, lo enmascara aunque el nombre no sea sensible.</li>
 * </ul>
 */
public final class SensitiveDataMasker {

    private static final String MASK = "***";

    private static final Set<String> DEFAULT_SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "clave", "contrasena", "contraseña", "pass",
            "token", "access_token", "refresh_token", "authorization", "bearer",
            "api_key", "apikey", "secret", "client_secret",
            "pin", "otp", "cvv", "cvc",
            "dni", "documento", "ruc", "pasaporte",
            "nro_cuenta", "numero_cuenta", "cuenta", "cci", "iban", "pan", "tarjeta",
            "correo", "email", "telefono", "celular", "direccion"
    ));

    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)^bearer\\s+.+$");
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9-_]+=*\\.[A-Za-z0-9-_]+=*\\.[A-Za-z0-9-_+/=]*$");

    private SensitiveDataMasker() {
    }

    public static List<Parameter> maskParameters(List<Parameter> original) {
        if (original == null || original.isEmpty()) {
            return original;
        }

        List<Parameter> masked = new ArrayList<>(original.size());
        for (Parameter p : original) {
            if (p == null) {
                continue;
            }
            String name = p.getName();
            String value = p.getValue();

            Parameter copy = new Parameter();
            copy.setName(name);
            copy.setMode(p.getMode());

            if (isSensitiveKey(name) || isSensitiveValue(value)) {
                copy.setValue(MASK);
            } else {
                copy.setValue(value);
            }

            masked.add(copy);
        }
        return masked;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) return false;

        String normalized = normalize(key);
        if (normalized.isEmpty()) return false;

        for (String sensitive : allSensitiveKeys()) {
            if (normalized.contains(normalize(sensitive))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSensitiveValue(String value) {
        if (value == null || value.isBlank()) return false;

        String trimmed = value.trim();
        if (BEARER_PATTERN.matcher(trimmed).matches()) return true;
        if (JWT_PATTERN.matcher(trimmed).matches()) return true;

        String configuredPassword = ConfigManager.getTestPassword();
        return !configuredPassword.isBlank() && configuredPassword.equals(trimmed);
    }

    private static Set<String> allSensitiveKeys() {
        Set<String> all = new HashSet<>(DEFAULT_SENSITIVE_KEYS);
        String extra = System.getProperty("report.mask.keys", "");
        if (!extra.isBlank()) {
            Arrays.stream(extra.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(all::add);
        }
        return all;
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }
}
