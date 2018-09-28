package cl.netgamer.villageinfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin
{
	// properties
	private Map<String, Object> worlds = new HashMap<String, Object>();
	private boolean usePermissions;
	private Map<String, Object> lang;
	private String version = getServer().getClass().getName().split("\\.")[3];
	
	// plugin load
	public void onEnable()
	{
		// get config
		this.saveDefaultConfig();
		usePermissions = getConfig().getBoolean("usePermissions");
		lang = getConfig().getConfigurationSection("msg").getValues(false);
		getLogger().info("Using permissions: "+usePermissions);		
		
		// create a map of WorldServers by its name, for later quick search
		try
		{
			// up to mc 1.12.2 (java 7): Method.invoke() didn't need an instance, CraftServer.getWorlds() method didn't exist yet
			//Object minecraftServer = Class.forName("net.minecraft.server."+version+".MinecraftServer").getMethod("getServer").invoke(null);
			//for (Object worldServer : (List<Object>) minecraftServer.getClass().getField("worlds").get(minecraftServer))
			
			Object minecraftServer = Class.forName("org.bukkit.craftbukkit."+version+".CraftServer").getMethod("getServer").invoke(this.getServer());
			for (Object worldServer : (Iterable) minecraftServer.getClass().getMethod("getWorlds").invoke(minecraftServer))
			{
				Object craftWorld = worldServer.getClass().getSuperclass().getMethod("getWorld").invoke(worldServer);
				this.worlds.put((String) craftWorld.getClass().getMethod("getName").invoke(craftWorld), worldServer);
			}
		}
		catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| SecurityException e){e.printStackTrace();}
	}
	
	// command
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args)
	{
		// previous checks
		if (!(cmd.getName().equalsIgnoreCase("villageinfo")))
			return true;
		
		if (!(sender instanceof Player))
		{
			sender.sendMessage("\u00A7D"+msg("onlyPlayers"));
			return true;
		}
		
		Player player = (Player)sender;
		
		if (usePermissions && !player.hasPermission("villageinfo.use") && !player.isOp()){
			player.sendMessage("\u00A7D"+msg("forbidden"));
			return true;
		}
		
		printVillageInfo(player);
		return true;
	}
	
	// core method
	void printVillageInfo(Player player)
	{
		// previous checks
		Location pLoc = player.getLocation();
		Location vCen;
		int rad = 0;
		
		// iterate villages from the world where player is
		for (Object vil: getVillagesByWorldName(pLoc.getWorld().getName()))
		{
			try
			{
				// get village center and radius and guess if player is inside
				vCen = getCenter(vil, pLoc.getWorld());
				rad = (int) vil.getClass().getMethod("b").invoke(vil);
				if (pLoc.distanceSquared(vCen) > (rad * rad))
					continue;
				
				/*
				Center: +234+54-324, Radius: 32		// informational: always yellow
				Houses: 8							// red=not enough houses for golems, blue=enough
				Villagers: 7 (max 3)				// red=no mating season, blue=mating
				Golems: 2 (max 3)					// blue=inside spawn
				Reputation: 4						// red=hated, blue=loved
				*/
				
				// for less redundancy
				int vX = vCen.getBlockX();
				int vY = vCen.getBlockY();
				int vZ = vCen.getBlockZ();
				boolean insideGolems = Math.abs(pLoc.getBlockX()-vX)<=8 && Math.abs(pLoc.getBlockY()-vY)<=3 && Math.abs(pLoc.getBlockZ()-vZ)<=8;
				Class<?> villageClass = vil.getClass();
				
				// get village info
				int doors = (int) villageClass.getMethod("c").invoke(vil);
				int people = (int) villageClass.getMethod("e").invoke(vil);
				int maxPeople = (int)(doors * 0.35D);
				int golems = countGolems(vil);
				int maxGolems = people / 10;
				int rep = (int) villageClass.getMethod("a", String.class).invoke(vil, player.getName());
				boolean enoughDoors = (doors > 20); // cyan|magenta
				boolean lowRep = (boolean) villageClass.getMethod("d", String.class).invoke(vil, player.getName()); // magenta|cyan
				boolean breeding = (boolean) villageClass.getMethod("i").invoke(vil);
				
				// print (\u00A7B=cyan, \u00A7D=magenta, \u00A7E=yellow, \u00A7 = section sign)
				player.sendMessage(
					"\u00A7E"+msg("center")+": "+(vX<0?"":"+")+vX+(vY<0?"":"+")+vY+(vZ<0?"":"+")+vZ+",  "+msg("radius")+": "+rad+"\n"+
					(enoughDoors?"\u00A7B":"\u00A7D")+msg("houses")+": "+doors+"\n"+
					(breeding?"\u00A7B":"\u00A7D")+msg("villagers")+": "+people+" ("+msg("max")+" "+maxPeople+")"+"\n"+
					(insideGolems?"\u00A7B":"\u00A7D")+msg("golems")+": "+golems+" ("+msg("max")+" "+maxGolems+")"+"\n"+
					(lowRep?"\u00A7D":"\u00A7B")+msg("reputation")+": "+rep);
			}
			catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException
					| SecurityException e){e.printStackTrace();}
			return;
		}
		// not inside a village
		player.sendMessage("\u00A7D"+msg("notInVillage"));
	}
	
	private List<Object> getVillagesByWorldName(String worldName)
	{
		try
		{
			// get declared field, since is not public?
			Field villages = Class.forName("net.minecraft.server."+version+".World").getDeclaredField("villages");
			villages.setAccessible(true);
			Object persistentVillageInstance = villages.get(worlds.get(worldName));
			if (persistentVillageInstance == null)
				return new ArrayList<Object>();
			return (List<Object>) Class.forName("net.minecraft.server."+version+".PersistentVillage").getMethod("getVillages").invoke(persistentVillageInstance);
		}
		catch (NullPointerException | NoSuchFieldException |
			IllegalArgumentException | IllegalAccessException |
			InvocationTargetException | NoSuchMethodException |
			SecurityException | ClassNotFoundException e){e.printStackTrace();}
		return new ArrayList<Object>();
	}
	
	// localization method
	private String msg(String key)
	{
		if (!(lang.containsKey(key))) return key;
		return (String)lang.get(key);
	}
	
	// get village center
	private Location getCenter(Object village, World w)
	{
		try
		{
			Object blockPosition = village.getClass().getMethod("a").invoke(village);
			int x = (int) blockPosition.getClass().getMethod("getX").invoke(blockPosition);
			int y = (int) blockPosition.getClass().getMethod("getY").invoke(blockPosition);
			int z = (int) blockPosition.getClass().getMethod("getZ").invoke(blockPosition);
			return new Location(w, x, y, z);
		}
		catch (IllegalAccessException | IllegalArgumentException
			| InvocationTargetException | NoSuchMethodException
			| SecurityException e){e.printStackTrace();}
		return new Location(w, 0, 0, 0);
	}
	
	// count golems near village
	protected static int countGolems(Object v)
	{
		try
		{
			Field l = v.getClass().getDeclaredField("l");
			l.setAccessible(true);
			return l.getInt(v);
		}
		catch (SecurityException | IllegalAccessException
			| IllegalArgumentException | NoSuchFieldException e){e.printStackTrace();}
		return -1;
	}
}
