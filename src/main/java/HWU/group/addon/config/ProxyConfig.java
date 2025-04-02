package HWU.group.addon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static HWU.group.addon.helpers.Utils.getGameDirectory;

public class ProxyConfig {
    private final Map<String, String> proxies = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final File file;
    private final Gson gson;

    public ProxyConfig() {
        this.file = new File(getGameDirectory(), "proxy_config.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public void addProxy(String name, String ipPort, boolean displayMessage) {
        load();
        if (proxies.containsKey(name)) {
            if (displayMessage)
                ChatUtils.errorPrefix("Proxy Switcher", "A proxy with that name already exists.");
            return;
        }
        proxies.put(name, ipPort);
        save();
        if (displayMessage)
            ChatUtils.infoPrefix("Proxy Switcher", "Added proxy \"%s\" with IP: %s.", name, ipPort);
    }

    public void removeProxy(String name, boolean displayMessage) {
        load();
        if (!proxies.containsKey(name)) {
            if (displayMessage)
                ChatUtils.errorPrefix("Proxy Switcher", "No proxy found with that name.");
            return;
        }
        proxies.remove(name);
        save();
        if (displayMessage)
            ChatUtils.infoPrefix("Proxy Switcher", "Removed proxy \"%s\".", name);
    }

    public void editUsername(String oldName, String newName) {
        load();

        if (!proxies.containsKey(oldName)) {
            // Show error if the old username is not found.
            ChatUtils.errorPrefix("Proxy Switcher", "No proxy found with the name \"%s\"", oldName);
            return;
        }
        if (proxies.containsKey(newName)) {
            ChatUtils.errorPrefix("Proxy Switcher", "A proxy with the name \"%s\" already exists.", newName);
            return;
        }

        String ipPort = proxies.remove(oldName);
        this.removeProxy(oldName, false);
        this.addProxy(newName, ipPort, false);
        save();
        ChatUtils.infoPrefix("Proxy Switcher", "Renamed proxy from \"%s\" to \"%s.\"", oldName, newName);
    }

    public void editServerIp(String name, String newIpPort) {
        load();
        if (!proxies.containsKey(name)) {
            ChatUtils.errorPrefix("Proxy Switcher", "No proxy found with the name \"%s\"", name);
            return;
        }

        this.removeProxy(name, false);
        this.addProxy(name, newIpPort, false);
        save();
        ChatUtils.infoPrefix("Proxy Switcher", "Updated server IP for proxy \"%s\" to %s.", name, newIpPort);
    }

    public List<String> listProxies() {
        load();
        List<String> proxyList = new ArrayList<>();

        if (proxies.isEmpty()) {
            ChatUtils.infoPrefix("Proxy Switcher", "No proxies found.");
            return proxyList;
        }

        proxies.forEach((name, ipPort) -> {
            String proxyInfo = String.format("%s: %s", name, ipPort);
            proxyList.add(proxyInfo);
        });

        return proxyList;
    }

    private void load() {
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Map<String, String> loadedProxies = gson.fromJson(reader, Map.class);
            if (loadedProxies != null) {
                proxies.clear();
                proxies.putAll(loadedProxies);
            }
        } catch (IOException | JsonSyntaxException e) {
            ChatUtils.errorPrefix("Proxy Switcher", "Failed to load proxies from JSON file: " + e.getMessage());
        }
    }

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(gson.toJson(proxies));
        } catch (IOException e) {
            ChatUtils.errorPrefix("Proxy Switcher", "Failed to save proxies to JSON file: " + e.getMessage());
        }
    }

    public void clearProxies() {
        proxies.clear();
        save();
        ChatUtils.infoPrefix("Proxy Switcher", "All proxies have been cleared.");
    }
}
