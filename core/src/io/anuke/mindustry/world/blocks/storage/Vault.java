package io.anuke.mindustry.world.blocks.storage;
import io.anuke.ucore.core.Settings;

public class Vault extends StorageBlock{

    public Vault(String name){
        super(name);
        solid = true;
        update = false;
        destructible = true;
        itemCapacity = Settings.getInt("capacitySlider3",1000);
    }

}
