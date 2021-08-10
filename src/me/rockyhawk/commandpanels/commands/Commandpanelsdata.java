package me.rockyhawk.commandpanels.commands;

import me.rockyhawk.commandpanels.CommandPanels;
import me.rockyhawk.commandpanels.commandtags.CommandTagEvent;
import me.rockyhawk.commandpanels.openpanelsmanager.PanelPosition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.UUID;

public class Commandpanelsdata implements CommandExecutor {
    CommandPanels plugin;
    public Commandpanelsdata(CommandPanels pl) { this.plugin = pl; }

    @EventHandler
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.hasPermission("commandpanel.data")) {
            if(args.length == 2){
                //for the clear command
                if(args[0].equals("clear")){
                    plugin.panelData.clearData(plugin.panelData.getOffline(args[1]));
                    sender.sendMessage(plugin.tex.colour(plugin.tag
                            + ChatColor.GREEN + "Cleared all data for "
                            + ChatColor.WHITE + args[1]));
                    return  true;
                }
            }else if (args.length == 3){
                //for the remove command
                if(args[0].equals("remove")) {
                    plugin.panelData.delUserData(plugin.panelData.getOffline(args[1]), args[2]);
                    sender.sendMessage(plugin.tex.colour(plugin.tag
                            + ChatColor.GREEN + "Removed "
                            + ChatColor.WHITE + args[2]
                            + ChatColor.GREEN + " from "
                            + ChatColor.WHITE + args[1]));
                    return  true;
                }else if(args[0].equals("get")){
                    //for the get command
                    sender.sendMessage(plugin.tex.colour(plugin.tag
                            + ChatColor.GREEN + "Value of data is "
                            + ChatColor.WHITE + plugin.panelData.getUserData(plugin.panelData.getOffline(args[1]), args[2])));
                    return  true;
                }
            }else if (args.length == 4){
                if(args[0].equals("set")){
                    //for set command
                    plugin.panelData.setUserData(plugin.panelData.getOffline(args[1]), args[2],args[3],true);
                    sender.sendMessage(plugin.tex.colour(plugin.tag
                            + ChatColor.GREEN + "Set "
                            + ChatColor.WHITE + args[2]
                            + ChatColor.GREEN + " to "
                            + ChatColor.WHITE + args[3]));
                    return  true;
                }else{
                    //for add command
                    plugin.panelData.setUserData(plugin.panelData.getOffline(args[1]), args[2],args[3],false);
                    sender.sendMessage(plugin.tex.colour(plugin.tag
                            + ChatColor.GREEN + "Set "
                            + ChatColor.WHITE + args[2]
                            + ChatColor.GREEN + " to "
                            + ChatColor.WHITE + args[3])
                            + ChatColor.GREEN + " if it did not exist already");
                    return  true;
                }
            }
            sender.sendMessage(plugin.tex.colour(plugin.tag + ChatColor.RED + "Usage: /cpdata <set:add:get:remove:clear> <player> <data> [value]"));
        }else{
            sender.sendMessage(plugin.tex.colour(plugin.tag + plugin.config.getString("config.format.perms")));
        }
        return true;
    }
}