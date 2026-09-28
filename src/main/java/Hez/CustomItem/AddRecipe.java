package Hez.CustomItem;

import Hez.CustomItem.Fish.FishData;
import Hez.CustomItem.Fish.FishManager;
import Hez.MineFarm.Main;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddRecipe {
    private final JavaPlugin plugin;

    public AddRecipe(JavaPlugin plugin){
        this.plugin = plugin;
    }

    FishManager fishManager = Main.getInstance().getFishManager();

    List<Material> eggList = new ArrayList<>();
    public void registarAll(){
        // 리스트들 등록
        addEggList();  // 달걀
        int index = 0; // 리스트 들어가는 요리

        // 요리 재료들
        ItemStack flour = CreateItem.create( Material.APPLE, // 밀가루
                "1001", "밀가루"
        );
        ItemStack fried_egg = CreateItem.create( Material.APPLE,
                "1009", "계란 후라이"
        );
        ItemStack hashbrowns = CreateItem.create( Material.APPLE,
                "1012", "해쉬브라운"
        );
        ItemStack pancakes = CreateItem.create( Material.APPLE,
                "1016", "팬케이크"
        );
        for (Material egg : eggList){
            // 계란 후라이
            ItemStack normalEgg = new ItemStack(egg);
            RecipeUntil.registerShapeless(
                    plugin,
                    "fried_egg_" + index,
                    Material.APPLE,
                    "1009",
                    "계란 후라이",
                    "fried_egg",
                    normalEgg
            );
            // 팬케이크
            RecipeUntil.registerShapeless(
                    plugin,
                    "pancakes_" + index,
                    Material.APPLE,
                    "1016",
                    "팬케이크",
                    "pancakes",
                    normalEgg,
                    normalEgg,
                    flour
            );
            RecipeUntil.registerShaped(
                    plugin,
                    "omelet_" + index++,
                    Material.APPLE,
                    "1015",
                    "오믈렛",
                    new String[]{
                            "E",
                            "H"
                    },
                    Map.of(
                            'E', normalEgg,
                            'H', new ItemStack(Material.MILK_BUCKET)
                    ),
                    "omelet"
            );
        }

        index = 0;

        // 물고기가 들어가는 음식(정해진 물고기 X)
        for (FishData fish : fishManager.fishByKey.values()) {
            RecipeUntil.registerShaped(
                    plugin,
                    "baked_fish_" + index,
                    Material.APPLE,
                    "1003",
                    "생선구이",
                    new String[]{
                            "B",
                            "E",
                    },
                    Map.of(
                            'B', flour,
                            'E', fish.createItemStack()

                    ),
                    "baked_fish"
            );
            RecipeUntil.registerShapeless(
                    plugin,
                    "sashimi_" + index++,
                    Material.APPLE,
                    "1021",
                    "회",
                    "sashimi",
                    fish.createItemStack()
            );
        }
        // 일반 요리들
        RecipeUntil.registerShaped(
                plugin,
                "salad",
                Material.APPLE,
                "1019",
                "샐러드",
                new String[] {
                        " B ",
                        "DEF",
                },
                Map.of(
                        'B', new ItemStack(Material.HONEY_BOTTLE),
                        'D', new ItemStack(Material.BEETROOT),
                        'E', new ItemStack(Material.POTATO),
                        'F', new ItemStack(Material.CARROT)
                ),
                null
        );
        RecipeUntil.registerShaped(
                plugin,
                "complet_blackfast",
                Material.APPLE,
                "1004",
                "완벽한 아침",
                new String[] {
                        "AB ",
                        "C E",
                },
                Map.of(
                        'A', fried_egg,
                        'B', hashbrowns,
                        'C', pancakes,
                        'E', new ItemStack(Material.MILK_BUCKET)
                ),null
        );
        RecipeUntil.registerShapeless(
                plugin,
                "fried_calamari",
                Material.APPLE,
                "1007",
                "오징어 튀김",
                null,
                fishManager.fish("오징어").createItemStack(),
                flour
        );
        RecipeUntil.registerShapeless(
                plugin,
                "fried_mushroom_0",
                Material.APPLE,
                "1010",
                "버섯구이",
                "fried_mushroom",
                new ItemStack(Material.BROWN_MUSHROOM),
                flour
        );
        RecipeUntil.registerShapeless(
                plugin,
                "fried_mushroom_1",
                Material.APPLE,
                "1010",
                "버섯구이",
                "fried_mushroom",
                new ItemStack(Material.RED_MUSHROOM),
                flour
        );
        RecipeUntil.registerShapeless(
                plugin,
                "hashbrowns",
                Material.APPLE,
                "1012",
                "해쉬브라운",
                null,
                new ItemStack(Material.POTATO),
                new ItemStack(Material.POTATO),
                new ItemStack(Material.POTATO)
        );
        RecipeUntil.registerShaped(
                plugin,
                "salmon_sashimi",
                Material.APPLE,
                "1020",
                "연어 회",
                new String[] {
                        "A",
                        "B"
                },
                Map.of(
                        'A', new ItemStack(Material.SALMON),
                        'B', new ItemStack(Material.SEAGRASS)
                ), null
        );
        RecipeUntil.registerShapeless(
                plugin,
                "crispy_bass",
                Material.APPLE,
                "1005",
                "우럭 튀김",
                null,
                fishManager.fish("큰입우럭").createItemStack(),
                flour,
                flour
        );
        RecipeUntil.registerShaped(
                plugin,
                "trout_soup",
                Material.APPLE,
                "1022",
                "송어 스프",
                new String[] {
                        "ABD",
                        " C "

                },
                Map.of(
                        'A', fishManager.fish("무지개송어").createItemStack(),
                        'B', new ItemStack(Material.KELP),
                        'C', new ItemStack(Material.BOWL),
                        'D', new ItemStack(Material.WATER_BUCKET)
                ),null
        );
        RecipeUntil.registerShapeless(
                plugin,
                "fried_eel",
                Material.APPLE,
                "1008",
                "장어 튀김",
                null,
                fishManager.fish("장어").createItemStack(),
                flour
        );
        RecipeUntil.registerShaped(
                plugin,
                "ice_cream",
                Material.APPLE,
                "1013",
                "아이스크림",
                new String[] {
                        "A",
                        "B"
                },
                Map.of(
                        'A', new ItemStack(Material.SUGAR),
                        'B', new ItemStack(Material.MILK_BUCKET)
                ), null
        );
        RecipeUntil.registerShapeless(
                plugin,
                "dish_of_sea",
                Material.APPLE,
                "1006",
                "바다의 요리",
                null,
                fishManager.fish("정어리").createItemStack(),
                fishManager.fish("정어리").createItemStack(),
                hashbrowns
        );
        RecipeUntil.registerShaped(
                plugin,
                "hamburger",
                Material.APPLE,
                "1002",
                "햄버거",
                new String[] {
                        " A ",
                        "BCD",
                        " A "
                },
                Map.of(
                        'A', new ItemStack(Material.BREAD),
                        'B', new ItemStack(Material.CARROT),
                        'C', new ItemStack(Material.COOKED_BEEF),
                        'D', new ItemStack(Material.KELP)
                ), null
        );
        RecipeUntil.registerShapeless(
                plugin,
                "flour",
                Material.APPLE,
                "1001",
                "밀가루",
                null,
                new ItemStack(Material.WHEAT)
        );
        RecipeUntil.registerShaped(
                plugin,
                "trout_soup",
                Material.APPLE,
                "1017",
                "설탕 당근 스프",
                new String[] {
                        "BCD",
                        " A "
                },
                Map.of(
                        'A', new ItemStack(Material.BOWL),
                        'B', new ItemStack(Material.CARROT),
                        'C', new ItemStack(Material.WATER_BUCKET),
                        'D', new ItemStack(Material.SUGAR)
                ), null
        );
        RecipeUntil.registerShaped(
                plugin,
                "gimbap",
                Material.APPLE,
                "1011",
                "김밥",
                new String[] {
                        " A ",
                        "BCD",
                        " A "
                },
                Map.of(
                        'A', new ItemStack(Material.DRIED_KELP),
                        'B', new ItemStack(Material.CARROT),
                        'C', new ItemStack(Material.COOKED_BEEF),
                        'D', new ItemStack(Material.POTATO)
                ), null
        );
        RecipeUntil.registerShaped(
                plugin,
                "meat_taco",
                Material.APPLE,
                "1014",
                "고기 타코",
                new String[] {
                        "E F",
                        "BCD",
                        " A "
                },
                Map.of(
                        'A', new ItemStack(Material.BREAD),
                        'B', new ItemStack(Material.COOKED_PORKCHOP),
                        'C', new ItemStack(Material.COOKED_BEEF),
                        'D', new ItemStack(Material.COOKED_MUTTON),
                        'E', new ItemStack(Material.KELP),
                        'F', new ItemStack(Material.POTATO)
                ), null
        );
        RecipeUntil.registerShapeless(
                plugin,
                "perfect_meal",
                Material.APPLE,
                "1018",
                "완벽한 아침",
                null,
                new ItemStack(Material.SWEET_BERRIES),
                new ItemStack(Material.GLOW_BERRIES),
                fishManager.fish("해삼").createItemStack(),
                new ItemStack(Material.GOLDEN_CARROT)
        );
    }

    private void addEggList(){
        eggList.add(Material.EGG);
        eggList.add(Material.BROWN_EGG);
        eggList.add(Material.BLUE_EGG);
    }
}
