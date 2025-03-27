package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class ErrorsDeleteModule implements Module {
    private final PhantCore plugin;
    private FileConfiguration config;
    private Logger logger;
    private LogFilter logFilter;

    public ErrorsDeleteModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ErrorsDelete";
    }

    @Override
    public void enable(PhantCore plugin) {
        // Загружаем конфигурацию
        File configFile = new File(plugin.getDataFolder(), "errorsdelete/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("errorsdelete/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // Получаем корневой логгер Log4j
        this.logger = (Logger) LogManager.getRootLogger();

        // Создаём и добавляем фильтр
        this.logFilter = new LogFilter(this);
        this.logger.addFilter(logFilter);

    }

    @Override
    public void disable(PhantCore plugin) {
    }

    public List<String> getFilteredMessages() {
        return config.getStringList("filtered-messages");
    }

    public PhantCore getPlugin() {
        return plugin;
    }
}

class LogFilter implements Filter {
    private final ErrorsDeleteModule module;
    private boolean suppressNextLines = false;

    public LogFilter(ErrorsDeleteModule module) {
        this.module = module;
    }

    @Override
    public Result filter(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        if (message == null) {
            return Result.NEUTRAL;
        }

        // Отладочный вывод для проверки, какие сообщения проходят через фильтр

        // Проверяем, нужно ли подавлять строки (стек вызовов или связанные сообщения)
        if (suppressNextLines) {
            // Подавляем строки, которые являются частью стека вызовов или связаны с ошибкой
            if (message.trim().startsWith("at ") || message.startsWith("\t") || message.startsWith(" ") ||
                    message.contains("Parameters:") || message.contains("net.minecraft.server.v1_16_R3.PacketPlayOutChat") ||
                    message.contains("Caused by:")) {
                return Result.DENY; // Продолжаем подавлять
            } else {
                // Сбрасываем флаг, если это независимая строка
                suppressNextLines = false;
            }
        }

        // Проверяем, содержит ли сообщение фильтруемую подстроку
        for (String filteredMessage : module.getFilteredMessages()) {
            if (message.contains(filteredMessage)) {
                suppressNextLines = true; // Начинаем подавлять последующие строки
                return Result.DENY; // Блокируем сообщение
            }
        }

        // Если сообщение не фильтруется, возвращаем NEUTRAL, чтобы оно отобразилось в консоли
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return checkMessage(msg.toString());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return checkMessage(msg.getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return checkMessage(msg);
    }

    private Result checkMessage(String message) {
        if (message == null) {
            return Result.NEUTRAL;
        }


        if (suppressNextLines) {
            if (message.trim().startsWith("at ") || message.startsWith("\t") || message.startsWith(" ") ||
                    message.contains("Parameters:") || message.contains("net.minecraft.server.v1_16_R3.PacketPlayOutChat") ||
                    message.contains("Caused by:")) {
                return Result.DENY;
            } else {
                suppressNextLines = false;
            }
        }

        for (String filteredMessage : module.getFilteredMessages()) {
            if (message.contains(filteredMessage)) {
                suppressNextLines = true;
                return Result.DENY;
            }
        }
        return Result.NEUTRAL;
    }

    @Override
    public State getState() {
        return State.STARTED;
    }

    @Override
    public void initialize() {}

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    @Override
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }
}