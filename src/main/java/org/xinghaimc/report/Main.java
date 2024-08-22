package org.xinghaimc.report;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private FileConfiguration reportConfig = null;
    private File reportsFile = null;

    @Override
    public void onEnable() {
        // 创建配置文件用于保存举报信息
        this.reportsFile = new File(this.getDataFolder(), "reports.yml");
        this.reportConfig = YamlConfiguration.loadConfiguration(this.reportsFile);

        // 加载或创建配置文件
        if (!this.reportsFile.exists()) {
            try {
                this.reportsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 注册命令
        registerCommands();
    }

    // 新增方法: 注册命令
    private void registerCommands() {
        // 获取 "report" 命令
        org.bukkit.command.PluginCommand reportCommand = getCommand("report");
        reportCommand.setExecutor(new ReportCommand());

        // 获取 "handle" 命令
        org.bukkit.command.PluginCommand handleCommand = getCommand("handle");
        handleCommand.setExecutor(new HandleCommand());
    }

    @Override
    public void onDisable() {
        // 保存并关闭配置文件
        try {
            reportConfig.save(reportsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 内部类: 处理/report命令
    private class ReportCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (cmd.getName().equalsIgnoreCase("report")) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (args.length < 2) {
                        player.sendMessage("正确用法: /report <玩家名> <原因>");
                        return true;
                    }

                    String targetName = args[0];
                    StringBuilder reason = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        reason.append(args[i]).append(" ");
                    }
                    reason.deleteCharAt(reason.length() - 1); // 移除最后一个空格

                    // 保存举报到配置文件
                    int reportId = reportConfig.getInt("lastReportId", 0) + 1;
                    reportConfig.set("reports." + reportId + ".reporter", player.getName());
                    reportConfig.set("reports." + reportId + ".target", targetName);
                    reportConfig.set("reports." + reportId + ".reason", reason.toString());
                    reportConfig.set("lastReportId", reportId);

                    // 通知玩家举报已提交
                    player.sendMessage("您的举报已被提交。");

                    // 将更改保存到文件
                    try {
                        reportConfig.save(reportsFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return true;
                } else {
                    sender.sendMessage("您必须在游戏中才能使用此命令。");
                }
            }
            return false;
        }
    }

    // 内部类: 处理/handle命令
    private class HandleCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (cmd.getName().equalsIgnoreCase("handle")) {
                if (sender.hasPermission("xinghai.report.handle")) {
                    if (args.length == 0) {
                        // 显示未处理的举报列表
                        List<String> reports = reportConfig.getKeys(false).stream()
                                .filter(key -> key.startsWith("reports."))
                                .toList();

                        if (reports.isEmpty()) {
                            sender.sendMessage("没有未处理的举报。");
                        } else {
                            sender.sendMessage("未处理的举报列表:");
                            for (String reportKey : reports) {
                                int id = Integer.parseInt(reportKey.split("\\.")[1]);
                                String reporter = reportConfig.getString(reportKey + ".reporter");
                                String target = reportConfig.getString(reportKey + ".target");
                                String reason = reportConfig.getString(reportKey + ".reason");
                                sender.sendMessage("ID: " + id + " - 举报者: " + reporter + " - 目标: " + target + " - 原因: " + reason);
                            }
                        }
                    } else if (args.length == 1) {
                        // 处理指定的举报
                        int reportId = Integer.parseInt(args[0]);

                        String reporter = reportConfig.getString("reports." + reportId + ".reporter");
                        String target = reportConfig.getString("reports." + reportId + ".target");
                        String reason = reportConfig.getString("reports." + reportId + ".reason");

                        if (reporter != null && target != null && reason != null) {
                            // 通知举报者
                            Player player = Bukkit.getPlayer(reporter);
                            if (player != null) {
                                player.sendMessage("您的举报(ID: " + reportId + ")已被处理。");
                            }

                            // 删除举报记录
                            reportConfig.set("reports." + reportId, null);

                            // 更新配置文件
                            try {
                                reportConfig.save(reportsFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            sender.sendMessage("举报(ID: " + reportId + ")已处理。");
                        } else {
                            sender.sendMessage("找不到举报ID: " + reportId);
                        }
                    } else {
                        sender.sendMessage("正确用法: /handle 或 /handle <举报ID>");
                    }
                } else {
                    sender.sendMessage("您没有权限执行此命令。");
                }
            }
            return false;
        }
    }
}