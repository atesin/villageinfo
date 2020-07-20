package cl.netgamer.villageinfo;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin
{
	// properties
	private boolean usePermissions;
	private Map<String, Object> messages;
	
	// plugin load
	public void onEnable()
	{
		// get config
		this.saveDefaultConfig();
		usePermissions = getConfig().getBoolean("usePermissions");
		messages = getConfig().getConfigurationSection("messages").getValues(false);
		getLogger().info("Using permissions: "+usePermissions);	
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
			sender.sendMessage("This command only makes sense for online players");
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
		Location pLoc = player.getEyeLocation();
		Location vCen = null; // geometric center of villagers found
		
		int villagers, golems, cats, animals, traders, illagers, zombies;
		villagers = golems = cats = animals = traders = illagers = zombies = 0;
		
		// scan entities first: no villagers = no village
		
		for (Entity mob : player.getNearbyEntities(64, 32, 64))
		{
			String mobClass = mob.getClass().getSimpleName();
			
			switch (mobClass)
			{
			case "CraftVillager":
			case "CraftVillagerZombie":
				if (vCen == null)
					vCen = mob.getLocation().subtract(pLoc);
				else
					vCen.add(mob.getLocation().subtract(pLoc));
				++villagers;
				continue;
			}
			
			switch (mobClass)
			{
			case "CraftPillager":
			case "CraftVindicator":
			case "CraftRavager":
			case "CraftEvoker":
			case "CraftVex":
			case "CraftIlusioner":
			case "CraftWitch":
				++illagers;
				continue;
			}
			
			switch (mobClass)
			{
			case "CraftCow":
			case "CraftHorse":
			case "CraftSheep":
			case "CraftPig":
				++animals;
				continue;
			}
			
			switch (mobClass)
			{
			case "CraftWanderingTrader":
			case "CraftTraderLlama":
				++traders;
				continue;
			}
			
			if (mobClass.equals("CraftIronGolem"))
				++golems;
			else if (mobClass.equals("CraftCat"))
				++cats;
			else if (mobClass.equals("CraftZombie"))
				++zombies;
		}
		
		if (villagers < 1)
		{
			player.sendMessage("\u00A7E"+msg("notInVillage"));
			return;
		}
		
		// scan blocks around village center
		
		vCen.multiply(1D / villagers).add(pLoc);
		int houses, sites;
		houses = sites = 0;
				
		for (Location loc : new BoxScanner(pLoc).scan())
		{
			String blockType = loc.getBlock().getType().toString();
			
			switch (blockType)
			{
			case "BLAST_FURNACE":     // armorer
			case "SMOKER":            // butcher
			case "CARTOGRAPHY_TABLE": // cartographer
			case "BREWING_STAND":     // cleric
			case "COMPOSTER":         // farmer
			case "BARREL":            // fisherman
			case "FLETCHING_TABLE":   // fletcher
			case "CAULDRON":          // leatherworker
			case "LECTERN":           // librarian
			case "STONECUTTER":       // mason
			case "LOOM":              // shepherd
			case "SMITHING_TABLE":    // toolsmith
			case "GRINDSTONE":        // weaponsmith
				++sites;
				continue;
			}
			
			if (blockType.endsWith("_BED"))
				++houses;
			//else if (blockType.equals("BELL"))
			//	++bells;
		}
		
		/*
		Legend                                       : caution state: magenta (normal state: cyan)
		---------------------------------------------------------------------------
		Village Center: front right above, 34 blocks : always yellow, informational
		Villagers: 9                                 : less than beds
		Nomad Traders: 3                             : none: cyan normal, any: cyan bold
		Golems: 2                                    : less than 1/5 villagers
		Cats: 5                                      : less than 1/4 beds
		Farm animals: 17                             : none (maybe were killed)
		Zombies: 12                                  : any (if many: possible siege in progress)
		Illagers: 5                                  : any (possible raid in progress)
		Houses (beds): 12                            : less than 10 (not enough to spawn golems)
		Job sites: 5                                 : less than than villagers
		*/
		
		player.sendMessage("\u00A7E"+msg("center")+": "+directionExplain(player.getEyeLocation(), vCen)+"\n\u00A7"+
			(villagers < houses      ? "D": "B")+msg("villagers")+": "+villagers+"\n\u00A7B"+
			(traders < 1        ? "": "\u00A7L")+msg("traders")+": "+traders+"\n\u00A7R\u00A7"+
			(golems < (villagers / 5)? "D": "B")+msg("golems")+": "+golems+"\n\u00A7"+
			(cats < (houses / 4)     ? "D": "B")+msg("cats")+": "+cats+"\n\u00A7"+
			(animals < 1             ? "D": "B")+msg("animals")+": "+animals+"\n\u00A7"+
			(zombies > 0             ? "D": "B")+msg("zombies")+": "+zombies+"\n\u00A7"+
			(illagers > 0            ? "D": "B")+msg("illagers")+": "+illagers+"\n\u00A7"+
			(houses < 9              ? "D": "B")+msg("houses")+": "+houses+"\n\u00A7"+
			(sites < villagers       ? "D": "B")+msg("sites")+": "+sites
		);
	}
	
	
	private String directionExplain(Location source, Location target)
	{
		double distance = fastDistance(source, target);
		if (distance < 5)
			return msg("here");
		
		// divide pitch in 5 regions:
		// above, yaw+above, yaw, yaw+below, below
		
		String explain = ", "+(int) distance+" "+msg("blocks");
		Location direction = source.clone().setDirection(target.clone().subtract(source).toVector());
		float pitch = Location.normalizePitch(direction.getPitch());
		
		if (pitch > 67.5)
			return msg("below")+explain;
		
		if (pitch < -67.5)
			return msg("above")+explain;
		
		if (pitch > 22.5)
			explain = " "+msg("below")+explain;
		else if (pitch < -22.5)
			explain = " "+msg("above")+explain;
		
		// divide yaw in 8 regions:
		// back, back-right, back-left, right, left, front-right, front-left, front
		
		float yaw = Location.normalizeYaw(direction.getYaw() - source.getYaw());
		
		if (yaw > 157.5 || yaw < -157.5)
			return msg("back")+explain;
		
		if (yaw > 112.5)
			return msg("back")+" "+msg("right")+explain;
		
		if (yaw < -112.5)
			return msg("back")+" "+msg("left")+explain;
		
		if (yaw > 67.5)
			return msg("right")+explain;
		
		if (yaw < -67.5)
			return msg("left")+explain;
		
		if (yaw > 22.5)
			return msg("front")+" "+msg("right")+explain;
		
		if (yaw < -22.5)
			return msg("front")+" "+msg("left")+explain;
		
		return msg("front")+explain;
	}
	
	
	private double fastDistance(Location a, Location b)
	{
		if (a.getWorld() != b.getWorld())
			throw new IllegalArgumentException();
		
		return fastDistance(fastDistance(b.getX() - a.getX(), b.getY() - a.getY()), b.getZ() - a.getZ());
	}
	
	
	private double fastDistance(double a, double b)
	{
		// https://math.stackexchange.com/questions/2533022/fast-approximated-hypotenuse-without-squared-root
		// m = x/y ... (0 <= x <= y / y != 0)
		// d ~= y (1 + 0.43*m^2)
		
		if ((a = Math.abs(a)) > (b = Math.abs(b)))
			a -= (b = (a += b) - b);
		
		if (a == 0)
			return b;
		
		double m = a / b;
		return b * (1 + (0.43 * m * m));
	}
	
	
	// localization method
	private String msg(String key)
	{
		if (messages.containsKey(key))
			return (String)messages.get(key);
		
		getLogger().warning("String not found in config: messages."+key);
		return key;
	}

}
