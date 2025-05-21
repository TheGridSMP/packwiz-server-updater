package sleepy_evelyn.packwizsu.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import sleepy_evelyn.packwizsu.PackWizSU;

@me.shedaniel.autoconfig.annotation.Config(name = PackWizSU.MOD_ID)
public class Config implements ConfigData {
    
    public String packLink;

    public String bootstrapJarName = "packwiz-installer-bootstrap.jar";
    public String bootstrapUrl = "https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/" + bootstrapJarName;
    public String bootstrapHash = "a8fbb24dc604278e97f4688e82d3d91a318b98efc08d5dbfcbcbcab6443d116c";

    public void save() {
        AutoConfig.getConfigHolder(Config.class).save();
    }
}
