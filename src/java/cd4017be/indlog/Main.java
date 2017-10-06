package cd4017be.indlog;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.lib.templates.TabMaterials;
import cd4017be.lib.util.TooltipUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Main.ID, useMetadata = true)
public class Main {

	public static final String ID = "indlog";
	static final String ConfigName = "inductiveLogistics";

	@Instance
	public static Main instance;

	@SidedProxy(serverSide = "cd4017be." + ID + ".CommonProxy", clientSide = "cd4017be." + ID + ".ClientProxy")
	public static CommonProxy proxy;

	public Main() {
		RecipeScriptContext.scriptRegistry.add(new Version(ConfigName, 18, "/assets/" + ID + "/config/recipes.rcp"));
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		TooltipUtil.CURRENT_DOMAIN = ID;
		Objects.tabIndLog = new TabMaterials(ID);
		Objects.createBlocks();
		Objects.createItems();
		Objects.registerCapabilities();
		RecipeScriptContext.instance.run(ConfigName + ".PRE_INIT");
		proxy.init();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {		
		proxy.registerRenderers();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

}
