package com.daldon.winterphenomena;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class WeatherCommands {
    private WeatherCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var seasonSet = Commands.literal("set").requires(source -> source.hasPermission(2));
        for (Season season : Season.values()) {
            seasonSet.then(Commands.literal(season.id()).executes(ctx -> {
                var server = ctx.getSource().getServer();
                ClimateData data = ClimateData.get(server);
                data.setSeason(season, server.overworld().getGameTime(), server.overworld().random);
                ClimateEngine.changed(server);
                return reply(ctx.getSource(), "Season set to " + season.id());
            }));
        }

        var eventSet = Commands.literal("set").requires(source -> source.hasPermission(2));
        for (WeatherKind weather : WeatherKind.values()) {
            eventSet.then(Commands.literal(weather.id()).executes(ctx -> {
                var server = ctx.getSource().getServer();
                ClimateData data = ClimateData.get(server);
                data.setWeather(weather, server.overworld().getGameTime(), server.overworld().random);
                data.weatherLocked = true;
                data.setDirty();
                ClimateEngine.changed(server);
                return reply(ctx.getSource(), "Weather set to " + weather.id() + " (locked; /daldon event auto resumes)");
            }));
        }

        dispatcher.register(Commands.literal("daldon")
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("season")
                .then(seasonSet)
                .then(Commands.literal("next").requires(s -> s.hasPermission(2))
                    .executes(ctx -> nextSeason(ctx.getSource())))
                .then(Commands.literal("skip").requires(s -> s.hasPermission(2))
                    .executes(ctx -> nextSeason(ctx.getSource())))
                .then(Commands.literal("lock").requires(s -> s.hasPermission(2))
                    .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                        ClimateData data = ClimateData.get(ctx.getSource().getServer());
                        data.seasonLocked = BoolArgumentType.getBool(ctx, "enabled");
                        data.setDirty();
                        WeatherNetwork.broadcast(ctx.getSource().getServer());
                        return reply(ctx.getSource(), "Season locked: " + data.seasonLocked);
                    }))))
            .then(Commands.literal("event")
                .then(eventSet)
                .then(Commands.literal("timer").executes(ctx -> timer(ctx.getSource())))
                .then(Commands.literal("next").requires(s -> s.hasPermission(2))
                    .executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        ClimateData data = ClimateData.get(server);
                        data.weatherLocked = false;
                        data.advanceWeather(server.overworld().getGameTime(), server.overworld().random);
                        ClimateEngine.changed(server);
                        return reply(ctx.getSource(), "Next event started: " + data.weather.id());
                    }))
                .then(Commands.literal("auto").requires(s -> s.hasPermission(2))
                    .executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        ClimateData data = ClimateData.get(server);
                        data.weatherLocked = false;
                        data.scheduleWeather(server.overworld().getGameTime(), server.overworld().random);
                        data.setDirty();
                        WeatherNetwork.broadcast(server);
                        return reply(ctx.getSource(), "Automatic weather events enabled");
                    }))));
    }

    private static int nextSeason(CommandSourceStack source) {
        var server = source.getServer();
        ClimateData data = ClimateData.get(server);
        data.setSeason(data.season.next(), server.overworld().getGameTime(), server.overworld().random);
        ClimateEngine.changed(server);
        return reply(source, "Season advanced to " + data.season.id());
    }

    private static int timer(CommandSourceStack source) {
        var server = source.getServer();
        ClimateData data = ClimateData.get(server);
        if (data.weatherLocked) return reply(source, "Weather locked at " + data.weather.id() + "; no upcoming event");
        long ticks = Math.max(0, data.nextWeatherTick - server.overworld().getGameTime());
        return reply(source, "Next: " + data.nextWeather.id() + " in " + format(ticks));
    }

    private static int status(CommandSourceStack source) {
        var server = source.getServer();
        ClimateData data = ClimateData.get(server);
        long time = server.overworld().getGameTime();
        return reply(source, "Season: " + data.season.id() +
            (data.seasonLocked ? " [locked]" : " (next in " + format(Math.max(0, data.nextSeasonTick - time)) + ")") +
            " | Weather: " + data.weather.id() +
            (data.weatherLocked ? " [locked]" : " | Next: " + data.nextWeather.id() +
                " in " + format(Math.max(0, data.nextWeatherTick - time))));
    }

    private static String format(long ticks) {
        long seconds = ticks / 20;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return (hours > 0 ? hours + "h " : "") + minutes + "m " + secs + "s";
    }
    private static int reply(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal("[Daldon Weather] " + text), false);
        return 1;
    }
}
