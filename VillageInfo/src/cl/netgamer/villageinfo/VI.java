package cl.netgamer.villageinfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.logging.Logger;

//179 = import net.minecraft.server.v1_7_R3.MinecraftServer;
//183 = import net.minecraft.server.v1_8_R2.MinecraftServer;
import net.minecraft.server.v1_8_R2.MinecraftServer;
import net.minecraft.server.v1_8_R2.PersistentVillage;
import net.minecraft.server.v1_8_R2.Village;
import net.minecraft.server.v1_8_R2.WorldServer;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class VI extends JavaPlugin
{

	// properties
	//private Logger logger;
	private Map<String, WorldServer> worlds = new HashMap<String, WorldServer>();
	
	// utility method
	/* private void log(String msg)
	{
		logger.info(msg);
	} */
	
	// plugin load
	public void onEnable()
	{
		// utility
		//logger = getLogger();
		
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
		if (!(cmd.getName().equalsIgnoreCase("villageinfo")))
		{
			// not "villageinfo" command
			return true;
		}
		
		if (!(sender instanceof Player))
		{
			sender.sendMessage("Sorry, only for online players for now");
			return true;
		}
		
		sender.sendMessage(insideVillageText((Player)sender));
		return true;
	}
	
	private String insideVillageText(Player pla)
	{
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
				String msg = "\n§ECenter: ";
				msg += (cen.getBlockX()<0 ? "" : "+")+cen.getBlockX()+(cen.getBlockY()<0 ? "" : "+")+cen.getBlockY()+(cen.getBlockZ()<0 ? "" : "+")+cen.getBlockZ();
				msg += ", Radius: "+rad+"\n"+villageText(vil, pla);
				
				if (Math.abs(loc.getBlockX() - cen.getBlockX()) <= 8 && Math.abs(loc.getBlockY() - cen.getBlockY()) <= 3 && Math.abs(loc.getBlockZ()-cen.getBlockZ()) <= 8)
				{
					msg += ", Inside Golem Spawn Area";
				}
				
				return msg;
			}
		}
		
		return "§DYou are not inside a Village radius";
	}
	
	private String villageText(Village vil, Player pla)
	{
		/*
		Example from Village Info mod ttp://chunkbase.com/mods/village-info
		
		Villagers: XX (YY)
		Golems: XX (YY)
		Houses: XX [red|green]
		Your reputation: XX [green|red]
		Breeding: OK|Stopped
		*/
		int people = VillageUtil.getNumVillagers(vil);
		int doors = VillageUtil.getNumDoors(vil);
		int breedLimit = (int)(doors * 0.35D);
		int golems = VillageUtil.getNumGolems(vil, worlds.get(pla.getLocation().getWorld().getName()));
		int golemLimit = people / 10;
		boolean enoughDoors = (doors > 20); // cyan|magenta
		int rep = VillageUtil.getReputationForPlayer(vil, pla);
		boolean lowRep = VillageUtil.isPlayerReputationTooLow(vil, pla); // magenta|cyan
		boolean breed = VillageUtil.isMatingSeason(vil); // OK|Stopped
		
		// §B=cyan, §D=magenta, §E=yellow
		String msg = "Villagers: "+people+" ("+breedLimit+"), ";
		msg += "Golems: "+golems+" ("+golemLimit+")\n";
		msg += (enoughDoors ? "§B" : "§D")+"Houses: "+doors+"§E, ";
		msg += (lowRep ? "§D" : "§B")+"Your Reputation: "+rep+"§E\n";
		msg += "Breeding Status: "+(breed ? "OK" : "Stopped");
		
		return msg;
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

	// reflection methods
	
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
}
