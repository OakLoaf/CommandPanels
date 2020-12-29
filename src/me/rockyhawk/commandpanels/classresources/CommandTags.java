package me.rockyhawk.commandpanels.classresources;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.realized.tokenmanager.api.TokenManager;
import me.rockyhawk.commandpanels.CommandPanels;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Objects;

public class CommandTags {
    CommandPanels plugin;
    public CommandTags(CommandPanels pl) {
        this.plugin = pl;
    }

    @SuppressWarnings("deprecation")
    public void commandTags(Player p, String command, String commandRAW) {
        //commandRAW is without placeholders
        switch(command.split("\\s")[0]){
            case "server=":{
                //this contacts bungee and tells it to send the server change command
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(command.split("\\s")[1]);
                Player player = Bukkit.getPlayerExact(p.getName());
                assert player != null;
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                break;
            }
            case "cpc":{
                //this will close the current inventory
                p.closeInventory();
                break;
            }
            case "set-data=":{
                //this will overwrite data. set-data= [data point] [data value]
                plugin.panelData.setUserData(p.getUniqueId(),command.split("\\s")[1],command.split("\\s")[2],true);
                break;
            }
            case "add-data=":{
                //this will not overwrite existing data. add-data= [data point] [data value]
                plugin.panelData.setUserData(p.getUniqueId(),command.split("\\s")[1],command.split("\\s")[2],false);
                break;
            }
            case "math-data=":{
                //only works if data is number, goes math-data= [data point] [operator:number] eg, math-data= -1 OR /3
                plugin.panelData.doDataMath(p.getUniqueId(),command.split("\\s")[1],command.split("\\s")[2]);
                break;
            }
            case "clear-data=":{
                //will clear all data for player clear-data= [playerName]
                plugin.panelData.clearData(p.getUniqueId());
                break;
            }
            case "del-data=":{
                //this will remove data. del-data= [data point]
                plugin.panelData.delUserData(p.getUniqueId(),command.split("\\s")[1]);
                break;
            }
            case "give-item=":{
                //this will remove data. give-item= [custom item]
                ItemStack itm = plugin.itemCreate.makeItemFromConfig(plugin.openPanels.getOpenPanel(p.getName()).getConfigurationSection("custom-item." + command.split("\\s")[1]), p, true, true, false);
                p.getInventory().addItem(itm);
                break;
            }
            case "open=":{
                //if player uses open= it will open the panel, with the option to add custom placeholders
                String panelName = commandRAW.split("\\s")[1];
                String cmd = commandRAW.replace("open= " + panelName,"");
                panelName = plugin.papi(p,panelName);

                Character[] cm = ArrayUtils.toObject(cmd.toCharArray());
                for(int i = 0; i < cm.length; i++){
                    if(cm[i].equals('[')){
                        String contents = cmd.substring(i+1, i+cmd.substring(i).indexOf(']'));
                        //do not change the placeholder
                        String placeholder = contents.substring(0,contents.indexOf(':'));
                        //only convert placeholders for the value
                        String value = plugin.papi(p,contents.substring(contents.indexOf(':')+1));
                        plugin.customCommand.addCCP(panelName,p.getName(),placeholder,value);
                        i = i+contents.length()-1;
                    }
                }

                for(String[] tempName : plugin.panelNames){
                    if(tempName[0].equals(panelName)){
                        ConfigurationSection panelConfig = YamlConfiguration.loadConfiguration(new File(plugin.panelsf + File.separator + plugin.panelFiles.get(Integer.parseInt(tempName[1])))).getConfigurationSection("panels." + panelName);
                        if(plugin.openPanels.hasPanelOpen(p.getName())) {
                            plugin.openPanels.skipPanels.add(p.getName());
                        }
                        plugin.openVoids.openCommandPanel(p,p,panelName,panelConfig,false);
                        return;
                    }
                }
                break;
            }
            case "console=":{
                //if player uses console= it will perform command in the console
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("console=", "").trim());
                break;
            }
            case "placeholder=":{
                //if player uses placeholder= it will only change the placeholders for the panel
                String panelName = commandRAW.split("\\s")[1];
                String cmd = commandRAW.replace("placeholder= " + panelName,"");
                panelName = plugin.papi(p,panelName);

                Character[] cm = ArrayUtils.toObject(cmd.toCharArray());
                for(int i = 0; i < cm.length; i++){
                    if(cm[i].equals('[')){
                        String contents = cmd.substring(i+1, i+cmd.substring(i).indexOf(']'));
                        //do not change the placeholder
                        String placeholder = contents.substring(0,contents.indexOf(':'));
                        //only convert placeholders for the value
                        String value = plugin.papi(p,contents.substring(contents.indexOf(':')+1));
                        plugin.customCommand.editCCP(panelName,p.getName(),placeholder,value);
                        i = i+contents.length()-1;
                    }
                }
                break;
            }
            case "setitem=":{
                //if player uses setitem= [custom item] [slot] it will change the item slot to something, used for placeable items
                //make a section in the panel called "custom-item" then whatever the title of the item is, put that here
                ConfigurationSection panelCF = plugin.openPanels.getOpenPanel(p.getName());
                ItemStack s = plugin.itemCreate.makeItemFromConfig(panelCF.getConfigurationSection("custom-item." + command.split("\\s")[1]), p, true, true, true);
                p.getOpenInventory().getTopInventory().setItem(Integer.parseInt(command.split("\\s")[2]), s);
                break;
            }
            case "refresh":{
                //this will just do a standard panel refresh, animate is set to 0
                plugin.createGUI.openGui(null, p, plugin.openPanels.getOpenPanel(p.getName()), 0,0);
                break;
            }
            case "op=":{
                //if player uses op= it will perform command as op
                boolean isop = p.isOp();
                try {
                    p.setOp(true);
                    Bukkit.dispatchCommand(p,command.replace("op=", "").trim());
                    p.setOp(isop);
                } catch (Exception exc) {
                    p.setOp(isop);
                    plugin.debug(exc);
                    p.sendMessage(plugin.tag + plugin.papi( plugin.config.getString("config.format.error") + " op=: Error in op command!"));
                }
                break;
            }
            case "delay=":{
                //if player uses op= it will perform command as op
                final int delaySeconds = Integer.parseInt(command.split("\\s")[1]);
                String finalCommand = command.split("\\s",3)[2];
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        commandTags(p, finalCommand, commandRAW);
                        this.cancel();
                    }
                }.runTaskTimer(plugin, 20*delaySeconds, 20); //20 ticks == 1 second
                break;
            }
            case "buy=":{
                //if player uses buy= it will be eg. buy= <price> <item> <amount of item> <ID>
                try {
                    if (plugin.econ != null) {
                        if (plugin.econ.getBalance(p) >= Double.parseDouble(command.split("\\s")[1])) {
                            plugin.econ.withdrawPlayer(p, Double.parseDouble(command.split("\\s")[1]));
                            //if the message is empty don't send
                            if(!Objects.requireNonNull(plugin.config.getString("config.format.bought")).isEmpty()){
                                p.sendMessage(plugin.papi( plugin.tag + Objects.requireNonNull(plugin.config.getString("config.format.bought")).replaceAll("%cp-args%", command.split("\\s")[1])));
                            }
                            //legacy ID
                            byte id = 0;
                            if(plugin.legacy.isLegacy()) {
                                for (String argsTemp : command.split("\\s")) {
                                    if (argsTemp.startsWith("id:")) {
                                        id = Byte.parseByte(argsTemp.replace("id:", ""));
                                        break;
                                    }
                                }
                            }
                            if (p.getInventory().firstEmpty() >= 0) {
                                p.getInventory().addItem(new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[2])), Integer.parseInt(command.split("\\s")[3]),id));
                            } else {
                                Objects.requireNonNull(p.getLocation().getWorld()).dropItemNaturally(p.getLocation(), new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[2])), Integer.parseInt(command.split("\\s")[3]),id));
                            }
                        } else {
                            p.sendMessage(plugin.papi( plugin.tag + plugin.config.getString("config.format.needmoney")));
                        }
                    } else {
                        p.sendMessage(plugin.papi( plugin.tag + ChatColor.RED + "Buying Requires Vault and an Economy to work!"));
                    }
                } catch (Exception buy) {
                    plugin.debug(buy);
                    p.sendMessage(plugin.papi(plugin.tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                }
                break;
            }
            case "tokenbuy=":{
                //if player uses tokenbuy= it will be eg. tokenbuy= <price> <item> <amount of item> <ID>
                try {
                    if (plugin.getServer().getPluginManager().isPluginEnabled("TokenManager")) {
                        TokenManager api = (TokenManager) Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
                        assert api != null;
                        int balance = Integer.parseInt(Long.toString(api.getTokens(p).orElse(0)));
                        if (balance >= Double.parseDouble(command.split("\\s")[1])) {
                            api.removeTokens(p, Long.parseLong(command.split("\\s")[1]));
                            //if the message is empty don't send
                            if(!Objects.requireNonNull(plugin.config.getString("config.format.bought-token")).isEmpty()) {
                                p.sendMessage(plugin.papi( plugin.tag + Objects.requireNonNull(plugin.config.getString("config.format.bought-token")).replaceAll("%cp-args%", command.split("\\s")[1])));
                            }
                            //legacy ID
                            byte id = 0;
                            if(plugin.legacy.isLegacy()) {
                                for (String argsTemp : command.split("\\s")) {
                                    if (argsTemp.startsWith("id:")) {
                                        id = Byte.parseByte(argsTemp.replace("id:", ""));
                                        break;
                                    }
                                }
                            }
                            if (p.getInventory().firstEmpty() >= 0) {
                                p.getInventory().addItem(new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[2])), Integer.parseInt(command.split("\\s")[3]),id));
                            } else {
                                Objects.requireNonNull(p.getLocation().getWorld()).dropItemNaturally(p.getLocation(), new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[2])), Integer.parseInt(command.split("\\s")[3]),id));
                            }
                        } else {
                            p.sendMessage(plugin.papi( plugin.tag + plugin.config.getString("config.format.needmoney-token")));
                        }
                    } else {
                        p.sendMessage(plugin.papi( plugin.tag + ChatColor.RED + "Buying Requires TokenManager to work!"));
                    }
                } catch (Exception buy) {
                    plugin.debug(buy);
                    p.sendMessage(plugin.papi(plugin.tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                }
                break;
            }
            case "sell=":{
                //if player uses sell= it will be eg. sell= <cashback> <item> <amount of item> [enchanted:KNOCKBACK:1] [potion:JUMP]
                try {
                    if (plugin.econ != null) {
                        boolean sold = false;
                        for (int f = 0; f < p.getInventory().getSize(); f++) {
                            ItemStack itm = p.getInventory().getItem(f);
                            if (itm != null && itm.getType().equals(Material.matchMaterial(command.split("\\s")[2]))) {
                                //determine if the command contains parameters for extensions
                                String potion = "false";
                                for(String argsTemp : command.split("\\s")){
                                    if(argsTemp.startsWith("potion:")){
                                        potion = argsTemp.replace("potion:","");
                                        break;
                                    }
                                }
                                //legacy ID
                                byte id = -1;
                                if(plugin.legacy.isLegacy()) {
                                    for (String argsTemp : command.split("\\s")) {
                                        if (argsTemp.startsWith("id:")) {
                                            id = Byte.parseByte(argsTemp.replace("id:", ""));
                                            break;
                                        }
                                    }
                                }
                                //check to ensure any extensions are checked
                                try {
                                    if (!potion.equals("false")) {
                                        PotionMeta potionMeta = (PotionMeta) itm.getItemMeta();
                                        assert potionMeta != null;
                                        if (!potionMeta.getBasePotionData().getType().name().equalsIgnoreCase(potion)) {
                                            continue;
                                        }
                                    }
                                    if (id != -1) {
                                        if (itm.getDurability() != id) {
                                            continue;
                                        }
                                    }
                                }catch(Exception exc){
                                    //skip unless plugin.debug enabled
                                    plugin.debug(exc);
                                }
                                if (itm.getAmount() >= new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[2])), Integer.parseInt(command.split("\\s")[3])).getAmount()) {
                                    int amt = itm.getAmount() - new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[2])), Integer.parseInt(command.split("\\s")[3])).getAmount();
                                    itm.setAmount(amt);
                                    p.getInventory().setItem(f, amt > 0 ? itm : null);
                                    plugin.econ.depositPlayer(p, Double.parseDouble(command.split("\\s")[1]));
                                    sold = true;
                                    p.updateInventory();
                                    break;
                                }
                            }
                        }
                        if (!sold) {
                            p.sendMessage(plugin.papi( plugin.tag + plugin.config.getString("config.format.needitems")));
                        } else {
                            //if the message is empty don't send
                            if(!Objects.requireNonNull(plugin.config.getString("config.format.sold")).isEmpty()) {
                                p.sendMessage(plugin.papi( plugin.tag + Objects.requireNonNull(plugin.config.getString("config.format.sold")).replaceAll("%cp-args%", command.split("\\s")[1])));
                            }
                        }
                    } else {
                        p.sendMessage(plugin.papi( plugin.tag + ChatColor.RED + "Selling Requires Vault and an Economy to work!"));
                    }
                } catch (Exception sell) {
                    plugin.debug(sell);
                    p.sendMessage(plugin.papi(plugin.tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                }
                break;
            }
            case "tokensell=":{
                //if player uses tokensell= it will be eg. tokensell= <cashback> <item> <amount of item> [enchanted:KNOCKBACK:1] [potion:JUMP]
                try {
                    if (plugin.getServer().getPluginManager().isPluginEnabled("TokenManager")) {
                        TokenManager api = (TokenManager) Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
                        boolean sold = false;
                        for (int f = 0; f < p.getInventory().getSize(); f++) {
                            ItemStack itm = p.getInventory().getItem(f);
                            if (itm != null && itm.getType().equals(Material.matchMaterial(command.split("\\s")[2]))) {
                                //determine if the command contains parameters for extensions
                                String potion = "false";
                                for(String argsTemp : command.split("\\s")){
                                    if(argsTemp.startsWith("potion:")){
                                        potion = argsTemp.replace("potion:","");
                                    }
                                }
                                //legacy ID
                                byte id = -1;
                                if(plugin.legacy.isLegacy()) {
                                    for (String argsTemp : command.split("\\s")) {
                                        if (argsTemp.startsWith("id:")) {
                                            id = Byte.parseByte(argsTemp.replace("id:", ""));
                                            break;
                                        }
                                    }
                                }
                                //check to ensure any extensions are checked
                                try {
                                    if (!potion.equals("false")) {
                                        PotionMeta potionMeta = (PotionMeta) itm.getItemMeta();
                                        assert potionMeta != null;
                                        if (!potionMeta.getBasePotionData().getType().name().equalsIgnoreCase(potion)) {
                                            p.sendMessage(plugin.papi( plugin.tag + ChatColor.RED + "Your item has the wrong potion effect"));
                                            return;
                                        }
                                    }
                                    if (id != -1) {
                                        if (itm.getDurability() != id) {
                                            continue;
                                        }
                                    }
                                }catch(Exception exc){
                                    //skip if it cannot do unless plugin.debug is enabled
                                    plugin.debug(exc);
                                }
                                if (itm.getAmount() >= new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[2])), Integer.parseInt(command.split("\\s")[3])).getAmount()) {
                                    int amt = itm.getAmount() - new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[2])), Integer.parseInt(command.split("\\s")[3])).getAmount();
                                    itm.setAmount(amt);
                                    p.getInventory().setItem(f, amt > 0 ? itm : null);
                                    plugin.econ.depositPlayer(p, Double.parseDouble(command.split("\\s")[1]));
                                    assert api != null;
                                    api.addTokens(p, Long.parseLong(command.split("\\s")[1]));
                                    sold = true;
                                    p.updateInventory();
                                    break;
                                }
                            }
                        }
                        if (!sold) {
                            p.sendMessage(plugin.papi( plugin.tag + plugin.config.getString("config.format.needitems")));
                        } else {
                            //if the message is empty don't send
                            if(!Objects.requireNonNull(plugin.config.getString("config.format.sold-token")).isEmpty()) {
                                p.sendMessage(plugin.papi( plugin.tag + Objects.requireNonNull(plugin.config.getString("config.format.sold-token")).replaceAll("%cp-args%", command.split("\\s")[1])));
                            }
                        }
                    } else {
                        p.sendMessage(plugin.papi( plugin.tag + ChatColor.RED + "Selling Requires TokenManager to work!"));
                    }
                } catch (Exception sell) {
                    plugin.debug(sell);
                    p.sendMessage(plugin.papi(plugin.tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                }
                break;
            }
            case "msg=":{
                //if player uses msg= it will send the player a message
                p.sendMessage(command.replace("msg=", "").trim());
                break;
            }
            case "sound=":{
                //if player uses sound= it will play a sound (sound= [sound])
                try {
                    p.playSound(p.getLocation(), Sound.valueOf(command.split("\\s")[1]), 1F, 1F);
                } catch (Exception s) {
                    plugin.debug(s);
                    p.sendMessage(plugin.papi(plugin.tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                }
                break;
            }
            case "buycommand=":{
                //if player uses buycommand [price] [command]
                try {
                    if (plugin.econ != null) {
                        if (plugin.econ.getBalance(p) >= Double.parseDouble(command.split("\\s")[1])) {
                            plugin.econ.withdrawPlayer(p, Double.parseDouble(command.split("\\s")[1]));
                            //execute command under here
                            String commandp = command;
                            commandp = commandp.replace("buycommand=", "").trim();
                            String price = commandp.split(" ", 2)[0];
                            commandp = commandp.split(" ", 2)[1];
                            commandTags(p,commandp,commandRAW);
                            //if the message is empty don't send
                            if(!Objects.requireNonNull(plugin.config.getString("config.format.bought")).isEmpty()) {
                                p.sendMessage(plugin.papi( plugin.tag + Objects.requireNonNull(plugin.config.getString("config.format.bought")).replaceAll("%cp-args%", price)));
                            }
                        } else {
                            p.sendMessage(plugin.papi( plugin.tag + plugin.config.getString("config.format.needmoney")));
                        }
                    } else {
                        p.sendMessage(plugin.papi( plugin.tag + ChatColor.RED + "Buying Requires Vault and an Economy to work!"));
                    }
                } catch (Exception buyc) {
                    plugin.debug(buyc);
                    p.sendMessage(plugin.papi(plugin.tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                }
                break;
            }
            case "tokenbuycommand=":{
                //if player uses tokenbuycommand [price] [command]
                try {
                    if (plugin.getServer().getPluginManager().isPluginEnabled("TokenManager")) {
                        TokenManager api = (TokenManager) Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
                        assert api != null;
                        int balance = Integer.parseInt(Long.toString(api.getTokens(p).orElse(0)));
                        if (balance >= Double.parseDouble(command.split("\\s")[1])) {
                            api.removeTokens(p, Long.parseLong(command.split("\\s")[1]));
                            //execute command under here
                            String commandp = command;
                            commandp = commandp.replace("tokenbuycommand=", "").trim();
                            String price = commandp.split(" ", 2)[0];
                            commandp = commandp.split(" ", 2)[1];
                            commandTags(p,commandp,commandRAW);
                            //if the message is empty don't send
                            if(!Objects.requireNonNull(plugin.config.getString("config.format.bought-token")).isEmpty()) {
                                p.sendMessage(plugin.papi( plugin.tag + Objects.requireNonNull(plugin.config.getString("config.format.bought-token")).replaceAll("%cp-args%", price)));
                            }
                        } else {
                            p.sendMessage(plugin.papi( plugin.tag + plugin.config.getString("config.format.needmoney-token")));
                        }
                    } else {
                        p.sendMessage(plugin.papi( plugin.tag + ChatColor.RED + "Buying Requires Vault and an Economy to work!"));
                    }
                } catch (Exception buyc) {
                    plugin.debug(buyc);
                    p.sendMessage(plugin.papi(plugin.tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                }
                break;
            }
            case "teleport=":{
                //if player uses teleport= x y z (optional other player)
                if (command.split("\\s").length == 6) {
                    float x, y, z, yaw, pitch; //pitch is the heads Y axis and yaw is the X axis
                    x = Float.parseFloat(command.split("\\s")[1]);
                    y = Float.parseFloat(command.split("\\s")[2]);
                    z = Float.parseFloat(command.split("\\s")[3]);
                    yaw = Float.parseFloat(command.split("\\s")[4]);
                    pitch = Float.parseFloat(command.split("\\s")[5]);
                    p.teleport(new Location(p.getWorld(), x, y, z, yaw, pitch));
                } else if (command.split("\\s").length <= 4) {
                    float x, y, z;
                    x = Float.parseFloat(command.split("\\s")[1]);
                    y = Float.parseFloat(command.split("\\s")[2]);
                    z = Float.parseFloat(command.split("\\s")[3]);
                    p.teleport(new Location(p.getWorld(), x, y, z));
                } else {
                    try {
                        Player otherplayer = Bukkit.getPlayer(command.split("\\s")[4]);
                        float x, y, z;
                        x = Float.parseFloat(command.split("\\s")[1]);
                        y = Float.parseFloat(command.split("\\s")[2]);
                        z = Float.parseFloat(command.split("\\s")[3]);
                        assert otherplayer != null;
                        otherplayer.teleport(new Location(otherplayer.getWorld(), x, y, z));
                    } catch (Exception tpe) {
                        p.sendMessage(plugin.tag + plugin.config.getString("config.format.notitem"));
                    }
                }
                break;
            }
            case "sudo=":{
                //if player uses sudo= [command] to send a command as them
                p.chat( "/" + command.replaceAll("sudo=", "").trim());
                break;
            }
            case "send=":{
                //if player uses send= [message] to send a message as them
                p.chat( command.replaceAll("send=", "").trim());
                break;
            }
            case "stopsound=":{
                //if player uses stopsound= [sound]
                try {
                    p.stopSound(Sound.valueOf(command.split("\\s")[1]));
                } catch (Exception ss) {
                    plugin.debug(ss);
                    p.sendMessage(plugin.papi(plugin.tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                }
                break;
            }
            default:
                Bukkit.dispatchCommand(p, command);
        }
    }

    @SuppressWarnings("deprecation")
    public int commandPayWall(Player p, String command) { //return 0 means no funds, 1 is they passed and 2 means paywall is not this command
        String tag = plugin.config.getString("config.format.tag") + " ";
        switch(command.split("\\s")[0]){
            case "paywall=": {
                //if player uses paywall= [price]
                try {
                    if (plugin.econ != null) {
                        if (plugin.econ.getBalance(p) >= Double.parseDouble(command.split("\\s")[1])) {
                            plugin.econ.withdrawPlayer(p, Double.parseDouble(command.split("\\s")[1]));
                            //if the message is empty don't send
                            if(!plugin.config.getString("config.format.bought").isEmpty()) {
                                p.sendMessage(plugin.papi( tag + Objects.requireNonNull(plugin.config.getString("config.format.bought")).replaceAll("%cp-args%", command.split("\\s")[1])));
                            }
                            return 1;
                        } else {
                            p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.needmoney")));
                            return 0;
                        }
                    } else {
                        p.sendMessage(plugin.papi( tag + ChatColor.RED + "Paying Requires Vault and an Economy to work!"));
                        return 0;
                    }
                } catch (Exception buyc) {
                    plugin.debug(buyc);
                    p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                    return 0;
                }
            }
            case "tokenpaywall=": {
                //if player uses tokenpaywall= [price]
                try {
                    if (plugin.getServer().getPluginManager().isPluginEnabled("TokenManager")) {
                        TokenManager api = (TokenManager) Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
                        assert api != null;
                        int balance = Integer.parseInt(Long.toString(api.getTokens(p).orElse(0)));
                        if (balance >= Double.parseDouble(command.split("\\s")[1])) {
                            api.removeTokens(p, Long.parseLong(command.split("\\s")[1]));
                            //if the message is empty don't send
                            if(!Objects.requireNonNull(plugin.config.getString("config.format.bought-token")).isEmpty()) {
                                p.sendMessage(plugin.papi( tag + Objects.requireNonNull(plugin.config.getString("config.format.bought-token")).replaceAll("%cp-args%", command.split("\\s")[1])));
                            }
                            return 1;
                        } else {
                            p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.needmoney-token")));
                            return 0;
                        }
                    } else {
                        p.sendMessage(plugin.papi( tag + ChatColor.RED + "Paying TokenManager to work!"));
                        return 0;
                    }
                } catch (Exception buyc) {
                    plugin.debug(buyc);
                    p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                    return 0;
                }
            }
            case "item-paywall=": {
                //if player uses item-paywall= [Material] [Amount] [Id]
                //or player can use item-paywall= [custom-item]
                try {
                    short id = 0;
                    if(command.split("\\s").length == 4){
                        id = Short.parseShort(command.split("\\s")[3]);
                    }

                    //create the item to be removed
                    ItemStack sellItem;
                    if(command.split("\\s").length == 2) {
                        sellItem = plugin.itemCreate.makeItemFromConfig(plugin.openPanels.getOpenPanel(p.getName()).getConfigurationSection("custom-item." + command.split("\\s")[1]), p, true, true, false);
                    }else{
                        sellItem = new ItemStack(Objects.requireNonNull(Material.matchMaterial(command.split("\\s")[1])), Integer.parseInt(command.split("\\s")[2]), id);
                    }
                    //this is not a boolean because it needs to return an int
                    int removedItem = 0;

                    //loop through items in the inventory
                    for(ItemStack content : p.getInventory().getContents()){

                        if(content == null){
                            //skip slot if empty
                            continue;
                        }

                        if(command.split("\\s").length == 2){
                            //if item paywall is custom item
                            if(content.isSimilar(sellItem)){
                                content.setAmount(content.getAmount() - sellItem.getAmount());
                                p.updateInventory();
                                removedItem = 1;
                                break;
                            }

                            //if custom item is an mmo item (1.14+ for the API)
                            try {
                                if (plugin.getServer().getPluginManager().isPluginEnabled("MMOItems")) {
                                    String customItemMaterial = plugin.openPanels.getOpenPanel(p.getName()).getString("custom-item." + command.split("\\s")[1] + ".material");
                                    String mmoType = customItemMaterial.split("\\s")[1];
                                    String mmoID = customItemMaterial.split("\\s")[2];

                                    if (plugin.isMMOItem(sellItem,mmoType,mmoID) && sellItem.getAmount() <= content.getAmount()) {
                                        content.setAmount(content.getAmount() - sellItem.getAmount());
                                        p.updateInventory();
                                        removedItem = 1;
                                        break;
                                    }
                                }
                            }catch (Exception ex){
                                plugin.debug(ex);
                            }

                        }else {
                            //if the item is a standard material
                            if (content.getType() == sellItem.getType()) {
                                if (sellItem.getAmount() <= content.getAmount()) {
                                    content.setAmount(content.getAmount() - sellItem.getAmount());
                                    p.updateInventory();
                                    removedItem = 1;
                                    break;
                                }
                            }
                        }
                    }

                    //send message and return
                    if(removedItem == 0){
                        p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.needmoney")));
                    }else{
                        if(!Objects.requireNonNull(plugin.config.getString("config.format.sold")).isEmpty()) {
                            p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.sold")));
                        }
                    }
                    return removedItem;
                } catch (Exception buyc) {
                    plugin.debug(buyc);
                    p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                    return 0;
                }
            }
            case "xp-paywall=": {
                //if player uses xp-paywall= [price]
                try {
                    int balance = p.getLevel();
                    if (balance >= Integer.parseInt(command.split("\\s")[1])) {
                        p.setLevel(p.getLevel() - Integer.parseInt(command.split("\\s")[1]));
                        //if the message is empty don't send
                        if(!Objects.requireNonNull(plugin.config.getString("config.format.bought")).isEmpty()) {
                            p.sendMessage(plugin.papi( tag + Objects.requireNonNull(plugin.config.getString("config.format.bought")).replaceAll("%cp-args%", command.split("\\s")[1])));
                        }
                        return 1;
                    } else {
                        p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.needmoney")));
                        return 0;
                    }
                } catch (Exception buyc) {
                    plugin.debug(buyc);
                    p.sendMessage(plugin.papi( tag + plugin.config.getString("config.format.error") + " " + "commands: " + command));
                    return 0;
                }
            }
        }
        return 2;
    }
}
