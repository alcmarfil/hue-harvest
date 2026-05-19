package com.hueharvest.shared;

import java.net.*;
import java.util.Enumeration;

public class NetworkUtils {

    /**
     * Scans active network interfaces to find the site-local IPv4 address (e.g. 192.168.x.x, 10.x.x.x).
     * Falls back to localhost if none are active.
     */
    public static String getLocalNetworkIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            // Fallback
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * Converts an IPv4 address string (e.g. "192.168.1.15") into an 8-character uppercase room code (e.g. "C0A8010F").
     */
    public static String ipToRoomCode(String ip) {
        if (ip == null) return "OFFLINE";
        if (ip.equals("localhost") || ip.equals("127.0.0.1")) {
            // Get actual IP to generate code
            ip = getLocalNetworkIp();
        }
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return ip; // Return raw if not standard IPv4
            
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                int val = Integer.parseInt(part.trim());
                String hex = Integer.toHexString(val);
                if (hex.length() < 2) sb.append("0");
                sb.append(hex);
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            return ip;
        }
    }

    /**
     * Converts an 8-character room code back into an IPv4 address.
     * If the input is not an 8-character hex string, it is assumed to be a raw IP/hostname and returned as-is.
     */
    public static String roomCodeToIp(String code) {
        if (code == null) return "localhost";
        code = code.trim().toUpperCase();
        
        // If it's not 8 characters or contains non-hex characters, assume it's a raw IP or hostname
        if (code.length() != 8 || !code.matches("^[0-9A-F]{8}$")) {
            return code;
        }
        
        try {
            int[] parts = new int[4];
            for (int i = 0; i < 4; i++) {
                parts[i] = Integer.parseInt(code.substring(i * 2, i * 2 + 2), 16);
            }
            return parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        } catch (Exception e) {
            return code;
        }
    }
}
