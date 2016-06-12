package cl.netgamer.villageinfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// mc179 = import net.minecraft.server.v1_7_R3.*;
// mc183 = import net.minecraft.server.v1_8_R2.*;
// mc184-189 = import net.minecraft.server.v1_8_R3.*;
// mc190-193 = import net.minecraft.server.v1_9_R1.*;
import net.minecraft.server.v1_9_R2.MinecraftServer;
import net.minecraft.server.v1_9_R2.PersistentVillage;
import net.minecraft.server.v1_9_R2.Village;
import net.minecraft.server.v1_9_R2.WorldServer;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class VI extends JavaPlugin
{
	// properties
	private static Logger logger;
	private Map<String, WorldServer> worlds = new HashMap<String, WorldServer>();
	private boolean usePermissions;
	private Map<String, Object> lang;
	
	// utility method
	private static void log(String msg)
	{
		logger.info(msg);
	}
	
	// plugin load
	public void onEnable()
	{
		// utility
		logger = getLogger();
		
		// get config
		this.saveDefaultConfig();
		usePermissions = getConfig().getBoolean("usePermissions");
		lang = getConfig().getConfigurationSection("msg").getValues(false);
		VI.log("Using permissions: "+usePermissions);		
		
		// no events just 1 command below
		
		// fill an array with worlds references to easy get player world
	    for (WorldServer world : MinecraftServer.getServer().worlds)
	    {
	      worlds.put(world.getWorld().getName(), world);
	    }
	}
	
	// commands
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args)
	{
		// previous checks
		
		if (!(cmd.getName().equalsIgnoreCase("villageinfo")))
		{
			// not "villageinfo" command
			return true;
		}
		
		if (!(sender instanceof Player))
		{
			sender.sendMessage("§D"+msg("onlyPlayers"));
			return true;
		}
		
		Player pla = (Player)sender;
		
		//VI.log("usePerm, hasPerm, isOp = "+usePermissions+pla.hasPermission("villageinfo.use")+pla.isOp());
		if (usePermissions && !pla.hasPermission("villageinfo.use") && !pla.isOp()){
			pla.sendMessage("§D"+msg("forbidden"));
			return true;
		}
		
		// executing command
		
		// guess if player is inside a village
		// get villages list for this world first
		Location loc = pla.getLocation();
		Location cen;
		int rad;
		
		for (Village vil: getVillagesByWorldName(loc.getWorld().getName()))
		{
			// guess if player is inside a village
			// get center and radius first
			cen = VillageUtil.getCenter(vil, loc.getWorld());
			rad = VillageUtil.getRadius(vil);
			if (loc.distanceSquared(cen) <= (rad * rad))
			{
				/*
				SAMPLE OUTPUT:
												// new line
				Center: +234+54-324, Radius: 32
				Houses: 8						// red=not enough houses for golems, blue=enough
				Villagers: 7 (max 3)			// red=no mating, blue=mating
				Golems: 2 (max 3)				// blue=inside spawn
				Reputation: 4					// red=hated, blue=loved
				*/
				
				// just for more readability
				int cx = cen.getBlockX();
				int cy = cen.getBlockY();
				int cz = cen.getBlockZ();
				int doors = VillageUtil.getNumDoors(vil);
				int people = VillageUtil.getNumVillagers(vil);
				int maxPeople = (int)(doors * 0.35D);
				int golems = VillageUtil.getNumGolems(vil, worlds.get(pla.getLocation().getWorld().getName()));
				int maxGolems = people / 10;
				int rep = VillageUtil.getReputationForPlayer(vil, pla);
				boolean enoughDoors = (doors > 20); // cyan|magenta
				boolean lowRep = VillageUtil.isPlayerReputationTooLow(vil, pla); // magenta|cyan
				boolean breeding = VillageUtil.isMatingSeason(vil);
				boolean insideGolems = Math.abs(loc.getBlockX()-cx)<=8 && Math.abs(loc.getBlockY()-cy)<=3 && Math.abs(loc.getBlockZ()-cz)<=8;
				
				// §B=cyan, §D=magenta, §E=yellow
				// § = alt+21 (win), save as ansi
				pla.sendMessage("§E"+msg("center")+": "+(cx<0?"":"+")+cx+(cy<0?"":"+")+cy+(cz<0?"":"+")+cz+", "+msg("radius")+": "+rad);
				pla.sendMessage((enoughDoors?"§B":"§D")+msg("houses")+": "+doors);
				pla.sendMessage((breeding?"§B":"§D")+msg("villagers")+": "+people+" ("+msg("max")+" "+maxPeople+")");
				pla.sendMessage((insideGolems?"§B":"§E")+msg("golems")+": "+golems+" ("+msg("max")+" "+maxGolems+")");
				pla.sendMessage((lowRep?"§D":"§B")+msg("reputation")+": "+rep);
				return true;
			}
		}
		
		// not inside a village
		pla.sendMessage("§D"+msg("notInVillage"));
		return true;
	}
	
	private List<Village> getVillagesByWorldName(String w)
    {
    	Object obj = null;
    	WorldServer world = worlds.get(w);
    	try
    	{
    		Field villages = getClassField(WorldServer.class, "villages");
    		setFieldAccessible(villages);
    		obj = villages.get(world);
    	}
    	catch (Exception e){}

    	if (obj == null)
    	{
    		return new ArrayList<Village>();
    	}
    	return ((PersistentVillage)obj).getVillages();
    }

	// reflection method
    // returns specified class field, recursively to superclass if implements interface
	private Field getClassField(Class<?> cls, String field) throws NoSuchFieldException
	{
		try
		{
			return cls.getDeclaredField(field);
		}
		catch (NoSuchFieldException e)
		{
			Class<?> superClass = cls.getSuperclass();
			if (superClass == null)
			{
				throw e;
			}
			return getClassField(superClass, field);
		}
	}
	
	private void setFieldAccessible(Field field)
	{
		if ((!Modifier.isPublic(field.getModifiers())) || (!Modifier.isPublic(field.getDeclaringClass().getModifiers())))
		{
			field.setAccessible(true);
		}
	}
	
	// localization method
	private String msg(String key)
	{
		if (!(lang.containsKey(key))) return key;
		return (String)lang.get(key);
	}
}
