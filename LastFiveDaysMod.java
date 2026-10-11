package dev.lastfivedays;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class LastFiveDaysMod implements ModInitializer {
    private static final long FIVE_DAYS_TICKS = 5L * 24_000L;
    private static boolean started = false;
    private static boolean cured = false;
    private static long startTime = 0L;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("virus")
                .then(Commands.literal("start").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    startTime = server.overworld().getDayTime();
                    started = true;
                    cured = false;
                    ctx.getSource().sendSuccess(
                        () -> Component.literal("§cВспышка началась! У тебя 5 игровых дней. Найди компоненты и создай лекарство командой /virus cure."),
                        false
                    );
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    if (!started) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§eВирусный сценарий ещё не начат. Введи /virus start."), false);
                        return 1;
                    }
                    if (cured) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§aЛекарство найдено! Мир спасён."), false);
                        return 1;
                    }
                    long elapsed = Math.max(0L, ctx.getSource().getServer().overworld().getDayTime() - startTime);
                    long remaining = Math.max(0L, FIVE_DAYS_TICKS - elapsed);
                    long days = (remaining + 23_999L) / 24_000L;
                    ctx.getSource().sendSuccess(() -> Component.literal("§cДо конца осталось игровых дней: " + days + ". Компоненты: паучий глаз, бутылка мёда и слеза гаста."), false);
                    return 1;
                }))
                .then(Commands.literal("cure").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!started) {
                        ctx.getSource().sendFailure(Component.literal("Сначала начни сценарий: /virus start"));
                        return 0;
                    }
                    if (cured) {
                        ctx.getSource().sendSuccess(() -> Component.literal("Лекарство уже создано!"), false);
                        return 1;
                    }
                    if (ctx.getSource().getServer().overworld().getDayTime() - startTime >= FIVE_DAYS_TICKS) {
                        ctx.getSource().sendFailure(Component.literal("§4Слишком поздно! Пять дней прошли. Начни заново командой /virus start."));
                        return 0;
                    }
                    if (count(player, Items.SPIDER_EYE) < 1 ||
                        count(player, Items.HONEY_BOTTLE) < 1 ||
                        count(player, Items.GHAST_TEAR) < 1) {
                        ctx.getSource().sendFailure(Component.literal("§eНе хватает ингредиентов! Нужны: 1 паучий глаз, 1 бутылка мёда и 1 слеза гаста."));
                        return 0;
                    }
                    cured = true;
                    ctx.getSource().sendSuccess(() -> Component.literal("§aЛЕКАРСТВО СОЗДАНО! Ты остановил неизвестный вирус и спас мир!"), false);
                    return 1;
                }))
            )
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (started && !cured && server.overworld().getDayTime() - startTime >= FIVE_DAYS_TICKS) {
                started = false;
                server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§4ПЯТЬ ДНЕЙ ПРОШЛИ! Вирус победил. Чтобы повторить сценарий, введи /virus start."),
                    false
                );
            }
        });
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().count(item);
    }
}
