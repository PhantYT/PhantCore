/* Decompiler 97ms, total 3690ms, lines 168 */
package com.phantyt.utils.commands;

import com.phantyt.utils.modules.BlockParticlesModule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class BlockParticlesCommands implements CommandExecutor, TabCompleter {
   private final BlockParticlesModule module;
   private static final String PERMISSION_BASE = "blockparticles";
   private static final List<String> SUBCOMMANDS = List.of("create", "delete", "movehere");

   public BlockParticlesCommands(BlockParticlesModule module) {
      this.module = module;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(this.module.getMessage("only-players", new String[0]));
         return true;
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("blockparticles.use")) {
            player.sendMessage(this.module.getMessage("no-permission", new String[0]));
            return true;
         } else if (args.length == 0) {
            this.sendUsage(player);
            return true;
         } else {
            String subcommand = args[0].toLowerCase();
            byte var8 = -1;
            switch(subcommand.hashCode()) {
            case -1352294148:
               if (subcommand.equals("create")) {
                  var8 = 0;
               }
               break;
            case -1335458389:
               if (subcommand.equals("delete")) {
                  var8 = 1;
               }
               break;
            case -103826623:
               if (subcommand.equals("movehere")) {
                  var8 = 2;
               }
            }

            switch(var8) {
            case 0:
               return this.handleCreate(player, args);
            case 1:
               return this.handleDelete(player);
            case 2:
               return this.handleMoveHere(player);
            default:
               this.sendUsage(player);
               return true;
            }
         }
      }
   }

   private void sendUsage(Player player) {
      player.sendMessage(this.module.getMessage("usage", new String[]{player.hasPermission("blockparticles.create") ? "create" : "", player.hasPermission("blockparticles.delete") ? "delete" : "", player.hasPermission("blockparticles.movehere") ? "movehere" : ""}));
   }

   private boolean handleCreate(Player player, String[] args) {
      if (!player.hasPermission("blockparticles.create")) {
         player.sendMessage(this.module.getMessage("no-permission", new String[0]));
         return true;
      } else if (args.length < 3) {
         player.sendMessage(this.module.getMessage("usage-create", new String[0]));
         return true;
      } else {
         try {
            Particle particle = Particle.valueOf(args[1].toUpperCase());
            long interval = Long.parseLong(args[2]);
            if (interval <= 0L) {
               player.sendMessage(this.module.getMessage("invalid-interval", new String[]{"Interval must be positive"}));
               return true;
            }

            if (interval < 0L) {
               player.sendMessage(this.module.getMessage("invalid-interval", new String[]{"Minimum interval is 0 ticks"}));
               return true;
            }

            BlockParticlesModule var10000 = this.module;
            String var10002 = particle.name();
            var10000.setPendingAction(player, "create " + var10002 + " " + interval);
            player.sendMessage(this.module.getMessage("click-to-create", new String[]{particle.name(), String.valueOf(interval)}));
         } catch (NumberFormatException var6) {
            player.sendMessage(this.module.getMessage("invalid-interval", new String[]{var6.getMessage()}));
         } catch (IllegalArgumentException var7) {
            player.sendMessage(this.module.getMessage("invalid-effect", new String[]{args[1]}));
         }

         return true;
      }
   }

   private boolean handleDelete(Player player) {
      if (!player.hasPermission("blockparticles.delete")) {
         player.sendMessage(this.module.getMessage("no-permission", new String[0]));
         return true;
      } else {
         this.module.setPendingAction(player, "delete");
         player.sendMessage(this.module.getMessage("click-to-delete", new String[0]));
         return true;
      }
   }

   private boolean handleMoveHere(Player player) {
      if (!player.hasPermission("blockparticles.movehere")) {
         player.sendMessage(this.module.getMessage("no-permission", new String[0]));
         return true;
      } else {
         Location targetBlock = player.getTargetBlockExact(5) != null ? player.getTargetBlockExact(5).getLocation() : null;
         if (targetBlock == null) {
            player.sendMessage(this.module.getMessage("no-block-targeted", new String[0]));
            return true;
         } else {
            Location effectLocation = targetBlock.clone().add(0.5D, 1.0D, 0.5D);
            this.module.moveParticleEffect(player, effectLocation);
            return true;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (!(sender instanceof Player)) {
         return List.of();
      } else {
         Player player = (Player)sender;
         List<String> completions = new ArrayList();
         if (args.length == 1) {
            completions.addAll((Collection)SUBCOMMANDS.stream().filter((cmd) -> {
               return player.hasPermission("blockparticles." + cmd);
            }).filter((cmd) -> {
               return cmd.startsWith(args[0].toLowerCase());
            }).collect(Collectors.toList()));
         } else if (args.length == 2 && args[0].equalsIgnoreCase("create") && player.hasPermission("blockparticles.create")) {
            completions.addAll((Collection)Stream.of(Particle.values()).map((p) -> {
               return p.name().toLowerCase();
            }).filter((name) -> {
               return name.startsWith(args[1].toLowerCase());
            }).collect(Collectors.toList()));
         } else if (args.length == 3 && args[0].equalsIgnoreCase("create") && player.hasPermission("blockparticles.create")) {
            completions.addAll((Collection)List.of("5", "10", "20", "40").stream().filter((interval) -> {
               return interval.startsWith(args[2]);
            }).collect(Collectors.toList()));
         }

         return completions;
      }
   }
}
