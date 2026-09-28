package Hez.CustomItem.Fish;

import Hez.MineFarm.Main;
import Hez.MineFarm.OpGetMassage;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FishingListener implements Listener {
    FishManager fishManager = Main.getInstance().getFishManager();

    @EventHandler
    public void OnFish(PlayerFishEvent event){
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // 낚시찌에 있는 필요한 정보
            ItemStack changeFish;
            FishHook hook = event.getHook();
            Block hookBlock = hook.getLocation().getBlock();
            Biome hookBiome = hookBlock.getBiome();
            World hookWorld = hook.getWorld();
            long hookWorldTime = hookWorld.getTime();

            // 용암에서 낚시했을시 용암장어
            if (hookBlock.getType() == Material.LAVA) changeFish = fishManager.fish("용암장어").createItemStack();
            else changeFish = whatFishIsCatched(hookBiome, hookWorld, hookWorldTime);

            if(event.getCaught() instanceof Item caughtItem) {
                caughtItem.setItemStack(changeFish);
            }

        }
    }

    private ItemStack whatFishIsCatched(Biome biome, World world, long worldTime) {
        List<FishData> availableFish = new ArrayList<>();
        // 사용가능한 물고기 분류
        for(FishData fish : fishManager.fishByKey.values()) {
            // 시간, 날씨, 바이옴이 일치하면 리스트에 추가
            if (fish.isCatchableTime(worldTime) && fish.isCatchableWeather(world) && fish.isCatchaleBiome(biome))
                availableFish.add(fish);
        }

        // 가중치에 따른 물고기 선택
        double totalWeight = 0.0;
        for(FishData fish : availableFish) {
            totalWeight += fish.chance();
        }

        double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;

        for(FishData fish : availableFish) {
            randomValue -= fish.chance();
            if (randomValue < 0) return fish.createItemStack();
        }

        // 예외(나오면 안됌)
        return availableFish.get(availableFish.size() - 1).createItemStack(); // 나중에 쓰레기 나오게 변경
    }
}
