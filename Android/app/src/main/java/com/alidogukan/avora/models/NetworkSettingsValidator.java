package com.alidogukan.avora.models;

/** Pure IPv4 validation shared by the device information screen tests. */
public final class NetworkSettingsValidator {
    private NetworkSettingsValidator() { }

    public enum Error {
        NONE,
        ADDRESS_REQUIRED,
        ADDRESS_INVALID,
        SUBNET_INVALID,
        GATEWAY_REQUIRED,
        GATEWAY_INVALID,
        GATEWAY_OUTSIDE_SUBNET,
        DNS_REQUIRED,
        DNS_INVALID
    }

    public static Result validateStatic(String address, String subnetMask,
                                        String gateway, String primaryDns,
                                        String secondaryDns) {
        String ip = clean(address);
        String maskText = clean(subnetMask);
        String router = clean(gateway);
        String dns1 = clean(primaryDns);
        String dns2 = clean(secondaryDns);

        if (ip.isEmpty()) return Result.error(Error.ADDRESS_REQUIRED);
        long ipValue = parseIpv4(ip);
        if (!isUsableUnicast(ipValue)) return Result.error(Error.ADDRESS_INVALID);

        int prefix = prefixLength(maskText);
        if (prefix < 1 || prefix > 30) return Result.error(Error.SUBNET_INVALID);

        if (router.isEmpty()) return Result.error(Error.GATEWAY_REQUIRED);
        long gatewayValue = parseIpv4(router);
        if (!isUsableUnicast(gatewayValue) || gatewayValue == ipValue) {
            return Result.error(Error.GATEWAY_INVALID);
        }

        long mask = prefixMask(prefix);
        long network = ipValue & mask;
        long broadcast = network | (~mask & 0xffffffffL);
        if ((gatewayValue & mask) != network
                || gatewayValue == network || gatewayValue == broadcast
                || ipValue == network || ipValue == broadcast) {
            return Result.error(Error.GATEWAY_OUTSIDE_SUBNET);
        }

        if (dns1.isEmpty()) return Result.error(Error.DNS_REQUIRED);
        if (!isUsableUnicast(parseIpv4(dns1))
                || (!dns2.isEmpty() && !isUsableUnicast(parseIpv4(dns2)))) {
            return Result.error(Error.DNS_INVALID);
        }
        return Result.valid(ip, prefix, router, dns1, dns2);
    }

    static int prefixLength(String subnetMask) {
        long value = parseIpv4(subnetMask);
        if (value < 0L) return -1;
        boolean zeroSeen = false;
        int ones = 0;
        for (int bit = 31; bit >= 0; bit--) {
            boolean one = ((value >>> bit) & 1L) == 1L;
            if (one && zeroSeen) return -1;
            if (one) ones++; else zeroSeen = true;
        }
        return ones;
    }

    public static String subnetMaskForPrefix(int prefix) {
        if (prefix < 0 || prefix > 32) return "";
        long value = prefixMask(prefix);
        return ((value >>> 24) & 255) + "."
                + ((value >>> 16) & 255) + "."
                + ((value >>> 8) & 255) + "." + (value & 255);
    }

    private static long prefixMask(int prefix) {
        if (prefix == 0) return 0L;
        return (0xffffffffL << (32 - prefix)) & 0xffffffffL;
    }

    private static long parseIpv4(String value) {
        String[] parts = clean(value).split("\\.", -1);
        if (parts.length != 4) return -1L;
        long result = 0L;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) return -1L;
            for (int i = 0; i < part.length(); i++) {
                if (!Character.isDigit(part.charAt(i))) return -1L;
            }
            if (part.length() > 1 && part.charAt(0) == '0') return -1L;
            int octet;
            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException error) {
                return -1L;
            }
            if (octet < 0 || octet > 255) return -1L;
            result = (result << 8) | octet;
        }
        return result;
    }

    private static boolean isUsableUnicast(long value) {
        if (value < 0L) return false;
        int first = (int) ((value >>> 24) & 255);
        int second = (int) ((value >>> 16) & 255);
        return first > 0 && first < 224 && first != 127
                && !(first == 169 && second == 254)
                && value != 0xffffffffL;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Result {
        public final boolean valid;
        public final Error error;
        public final String ipAddress;
        public final int prefixLength;
        public final String gateway;
        public final String primaryDns;
        public final String secondaryDns;

        private Result(boolean valid, Error error, String ipAddress,
                       int prefixLength, String gateway, String primaryDns,
                       String secondaryDns) {
            this.valid = valid;
            this.error = error;
            this.ipAddress = ipAddress;
            this.prefixLength = prefixLength;
            this.gateway = gateway;
            this.primaryDns = primaryDns;
            this.secondaryDns = secondaryDns;
        }

        private static Result error(Error error) {
            return new Result(false, error, "", 0, "", "", "");
        }

        private static Result valid(String ip, int prefix, String gateway,
                                    String primaryDns, String secondaryDns) {
            return new Result(true, Error.NONE, ip, prefix, gateway,
                    primaryDns, secondaryDns);
        }
    }
}
