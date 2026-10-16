package Hez.Encyclopedia;

import Hez.MineFarm.Main;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DefaultEncyclopediaRegistry {

    private static final Map<Integer, EncyclopediaItem> ITEMS_BY_INDEX = new HashMap<>();
    private static final Map<String, EncyclopediaItem> ITEMS_BY_NAME = new HashMap<>();
    private static final Map<String, EncyclopediaItem> ITEMS_BY_KEY = new HashMap<>();
    private static final Map<Material, EncyclopediaItem> ITEMS_BY_MATERIAL = new HashMap<>();

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;

        // 1. 마인팜도감리스트.txt 읽기
        InputStream is = Main.getInstance().getResource("마인팜도감리스트.txt");
        if (is == null) {
            Main.getInstance().getLogger().warning("마인팜도감리스트.txt 리소스를 찾을 수 없습니다.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int dotIdx = line.indexOf('.');
                if (dotIdx == -1) continue;

                int index = Integer.parseInt(line.substring(0, dotIdx).trim());
                String name = line.substring(dotIdx + 1).trim();

                Material mat = resolveMaterial(index, name);
                EncyclopediaItem item = new EncyclopediaItem(index, name, mat);

                ITEMS_BY_INDEX.put(index, item);
                ITEMS_BY_NAME.put(name.replace(" ", ""), item);

                // 영문 키 매핑 등록
                String key = resolveInternalKey(index);
                if (key != null) {
                    ITEMS_BY_KEY.put(key.toLowerCase(), item);
                }

                // PAPER나 특수 공통 재료가 아닌 경우 Material 매핑에 등록
                if (mat != Material.PAPER && mat != Material.AIR && !ITEMS_BY_MATERIAL.containsKey(mat)) {
                    ITEMS_BY_MATERIAL.put(mat, item);
                }
            }
            initialized = true;
            Main.getInstance().getLogger().info("기본 도감 아이템 " + ITEMS_BY_INDEX.size() + "종 로드 완료.");
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("기본 도감 리스트 로드 중 오류: " + e.getMessage());
        }
    }

    public static EncyclopediaItem getItem(int index) {
        return ITEMS_BY_INDEX.get(index);
    }

    public static int getTotalItemCount() {
        return ITEMS_BY_INDEX.size();
    }

    /**
     * 플레이어가 클릭한 ItemStack에 해당하는 도감 아이템을 조회
     */
    public static EncyclopediaItem findMatchingItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;

        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                // 1. PDC(PersistentDataContainer) 태그 확인 (string 값들 검사)
                for (org.bukkit.NamespacedKey nKey : meta.getPersistentDataContainer().getKeys()) {
                    String val = meta.getPersistentDataContainer().get(nKey, org.bukkit.persistence.PersistentDataType.STRING);
                    if (val != null) {
                        EncyclopediaItem matched = ITEMS_BY_KEY.get(val.toLowerCase());
                        if (matched != null) return matched;
                        matched = ITEMS_BY_NAME.get(val.replace(" ", ""));
                        if (matched != null) return matched;
                    }
                }

                // 2. 커스텀 이름(DisplayName) 매칭
                if (meta.hasDisplayName()) {
                    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                            .serialize(meta.displayName())
                            .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                            .trim();

                    EncyclopediaItem matched = ITEMS_BY_NAME.get(plain.replace(" ", ""));
                    if (matched != null) return matched;

                    matched = ITEMS_BY_KEY.get(plain.toLowerCase().replace(" ", "_"));
                    if (matched != null) return matched;
                }
            }
        }

        // 3. 바닐라 Material 매칭
        return ITEMS_BY_MATERIAL.get(stack.getType());
    }

    private static String resolveInternalKey(int index) {
        return switch (index) {
            case 54 -> "poplar_log";
            case 55 -> "stripped_poplar_log";
            case 56 -> "poplar_planks";
            case 57 -> "poplar_sapling";
            case 58 -> "red_poplar_leaves";
            case 59 -> "orange_poplar_leaves";
            case 60 -> "yellow_poplar_leaves";
            case 323 -> "shelf_mushroom";
            case 362 -> "dried_ghast";
            case 382 -> "white_cushion";
            default -> null;
        };
    }

    /**
     * 1번~432번 도감 번호 및 한글명에 따라 적절한 Material 매핑
     */
    private static Material resolveMaterial(int index, String name) {
        return switch (index) {
            // 1 ~ 5 참나무
            case 1 -> Material.OAK_LOG;
            case 2 -> Material.STRIPPED_OAK_LOG;
            case 3 -> Material.OAK_PLANKS;
            case 4 -> Material.OAK_LEAVES;
            case 5 -> Material.OAK_SAPLING;
            // 6 ~ 10 가문비나무
            case 6 -> Material.SPRUCE_LOG;
            case 7 -> Material.STRIPPED_SPRUCE_LOG;
            case 8 -> Material.SPRUCE_PLANKS;
            case 9 -> Material.SPRUCE_LEAVES;
            case 10 -> Material.SPRUCE_SAPLING;
            // 11 ~ 15 자작나무
            case 11 -> Material.BIRCH_LOG;
            case 12 -> Material.STRIPPED_BIRCH_LOG;
            case 13 -> Material.BIRCH_PLANKS;
            case 14 -> Material.BIRCH_LEAVES;
            case 15 -> Material.BIRCH_SAPLING;
            // 16 ~ 20 정글나무
            case 16 -> Material.JUNGLE_LOG;
            case 17 -> Material.STRIPPED_JUNGLE_LOG;
            case 18 -> Material.JUNGLE_PLANKS;
            case 19 -> Material.JUNGLE_LEAVES;
            case 20 -> Material.JUNGLE_SAPLING;
            // 21 ~ 25 아카시아나무
            case 21 -> Material.ACACIA_LOG;
            case 22 -> Material.STRIPPED_ACACIA_LOG;
            case 23 -> Material.ACACIA_PLANKS;
            case 24 -> Material.ACACIA_LEAVES;
            case 25 -> Material.ACACIA_SAPLING;
            // 26 ~ 30 짙은참나무
            case 26 -> Material.DARK_OAK_LOG;
            case 27 -> Material.STRIPPED_DARK_OAK_LOG;
            case 28 -> Material.DARK_OAK_PLANKS;
            case 29 -> Material.DARK_OAK_LEAVES;
            case 30 -> Material.DARK_OAK_SAPLING;
            // 31 ~ 35 벚나무
            case 31 -> Material.CHERRY_LOG;
            case 32 -> Material.STRIPPED_CHERRY_LOG;
            case 33 -> Material.CHERRY_PLANKS;
            case 34 -> Material.CHERRY_LEAVES;
            case 35 -> Material.CHERRY_SAPLING;
            // 36 ~ 41 창백한나무 & 창백한이끼
            case 36 -> Material.PALE_OAK_LOG;
            case 37 -> Material.STRIPPED_PALE_OAK_LOG;
            case 38 -> Material.PALE_OAK_PLANKS;
            case 39 -> Material.PALE_OAK_LEAVES;
            case 40 -> Material.PALE_OAK_SAPLING;
            case 41 -> Material.PALE_MOSS_BLOCK;
            // 42 ~ 47 뒤틀린
            case 42 -> Material.WARPED_STEM;
            case 43 -> Material.STRIPPED_WARPED_STEM;
            case 44 -> Material.WARPED_PLANKS;
            case 45 -> Material.WARPED_WART_BLOCK;
            case 46 -> Material.WARPED_FUNGUS;
            case 47 -> Material.WARPED_NYLIUM;
            // 48 ~ 53 진홍빛
            case 48 -> Material.CRIMSON_STEM;
            case 49 -> Material.STRIPPED_CRIMSON_STEM;
            case 50 -> Material.CRIMSON_PLANKS;
            case 51 -> Material.NETHER_WART_BLOCK;
            case 52 -> Material.CRIMSON_FUNGUS;
            case 53 -> Material.CRIMSON_NYLIUM;
            // 54 ~ 60 포플러나무
            case 54 -> resolveDynamicMaterial("POPLAR_LOG", "OAK_LOG");
            case 55 -> resolveDynamicMaterial("STRIPPED_POPLAR_LOG", "STRIPPED_OAK_LOG");
            case 56 -> resolveDynamicMaterial("POPLAR_PLANKS", "OAK_PLANKS");
            case 57 -> resolveDynamicMaterial("POPLAR_SAPLING", "OAK_SAPLING");
            case 58 -> resolveDynamicMaterial("RED_POPLAR_LEAVES", "OAK_LEAVES");
            case 59 -> resolveDynamicMaterial("ORANGE_POPLAR_LEAVES", "OAK_LEAVES");
            case 60 -> resolveDynamicMaterial("YELLOW_POPLAR_LEAVES", "OAK_LEAVES");
            // 61 ~ 66 맹그로브나무
            case 61 -> Material.MANGROVE_LOG;
            case 62 -> Material.STRIPPED_MANGROVE_LOG;
            case 63 -> Material.MANGROVE_PLANKS;
            case 64 -> Material.MANGROVE_LEAVES;
            case 65 -> Material.MANGROVE_PROPAGULE;
            case 66 -> Material.MANGROVE_ROOTS;
            // 67 ~ 70 대나무
            case 67 -> Material.BAMBOO_BLOCK;
            case 68 -> Material.STRIPPED_BAMBOO_BLOCK;
            case 69 -> Material.BAMBOO_PLANKS;
            case 70 -> Material.BAMBOO;
            // 71 ~ 75 진달래 & 이끼
            case 71 -> Material.AZALEA_LEAVES;
            case 72 -> Material.FLOWERING_AZALEA_LEAVES;
            case 73 -> Material.AZALEA;
            case 74 -> Material.FLOWERING_AZALEA;
            case 75 -> Material.MOSS_BLOCK;
            // 76 ~ 89 돌, 벽돌, 암석류
            case 76 -> Material.STONE;
            case 77 -> Material.SMOOTH_STONE;
            case 78 -> Material.COBBLESTONE;
            case 79 -> Material.MOSSY_COBBLESTONE;
            case 80 -> Material.STONE_BRICKS;
            case 81 -> Material.CRACKED_STONE_BRICKS;
            case 82 -> Material.CHISELED_STONE_BRICKS;
            case 83 -> Material.MOSSY_STONE_BRICKS;
            case 84 -> Material.GRANITE;
            case 85 -> Material.POLISHED_GRANITE;
            case 86 -> Material.DIORITE;
            case 87 -> Material.POLISHED_DIORITE;
            case 88 -> Material.ANDESITE;
            case 89 -> Material.POLISHED_ANDESITE;
            // 90 ~ 97 심층암류
            case 90 -> Material.DEEPSLATE;
            case 91 -> Material.POLISHED_DEEPSLATE;
            case 92 -> Material.COBBLED_DEEPSLATE;
            case 93 -> Material.CHISELED_DEEPSLATE;
            case 94 -> Material.DEEPSLATE_BRICKS;
            case 95 -> Material.CRACKED_DEEPSLATE_BRICKS;
            case 96 -> Material.DEEPSLATE_TILES;
            case 97 -> Material.CRACKED_DEEPSLATE_TILES;
            // 98 ~ 107 응회암, 벽돌, 진흙, 수지
            case 98 -> Material.TUFF;
            case 99 -> Material.CHISELED_TUFF;
            case 100 -> Material.POLISHED_TUFF;
            case 101 -> Material.TUFF_BRICKS;
            case 102 -> Material.CHISELED_TUFF_BRICKS;
            case 103 -> Material.BRICKS;
            case 104 -> Material.PACKED_MUD;
            case 105 -> Material.MUD_BRICKS;
            case 106 -> Material.RESIN_BRICKS;
            case 107 -> Material.CHISELED_RESIN_BRICKS;
            // 108 ~ 115 사암
            case 108 -> Material.SANDSTONE;
            case 109 -> Material.SMOOTH_SANDSTONE;
            case 110 -> Material.CHISELED_SANDSTONE;
            case 111 -> Material.CUT_SANDSTONE;
            case 112 -> Material.RED_SANDSTONE;
            case 113 -> Material.SMOOTH_RED_SANDSTONE;
            case 114 -> Material.CHISELED_RED_SANDSTONE;
            case 115 -> Material.CUT_RED_SANDSTONE;
            // 116 ~ 125 진사, 유황
            case 116 -> Material.CINNABAR;
            case 117 -> Material.POLISHED_CINNABAR;
            case 118 -> Material.CHISELED_CINNABAR;
            case 119 -> Material.CINNABAR_BRICKS;
            case 120 -> Material.SULFUR;
            case 121 -> Material.POLISHED_SULFUR;
            case 122 -> Material.CHISELED_SULFUR;
            case 123 -> Material.SULFUR_BRICKS;
            case 124 -> Material.SULFUR_SPIKE;
            case 125 -> Material.POTENT_SULFUR;
            // 126 ~ 136 흙, 모래
            case 126 -> Material.DIRT;
            case 127 -> Material.GRASS_BLOCK;
            case 128 -> Material.COARSE_DIRT;
            case 129 -> Material.ROOTED_DIRT;
            case 130 -> Material.PODZOL;
            case 131 -> Material.MYCELIUM;
            case 132 -> Material.MUD;
            case 133 -> Material.CLAY;
            case 134 -> Material.GRAVEL;
            case 135 -> Material.SAND;
            case 136 -> Material.RED_SAND;
            // 137 ~ 145 얼음, 눈, 점적석, 버섯블록
            case 137 -> Material.ICE;
            case 138 -> Material.PACKED_ICE;
            case 139 -> Material.BLUE_ICE;
            case 140 -> Material.SNOW_BLOCK;
            case 141 -> Material.CALCITE;
            case 142 -> Material.DRIPSTONE_BLOCK;
            case 143 -> Material.POINTED_DRIPSTONE;
            case 144 -> Material.BROWN_MUSHROOM_BLOCK;
            case 145 -> Material.RED_MUSHROOM_BLOCK;
            // 146 ~ 174 광물류
            case 146 -> Material.COAL;
            case 147 -> Material.COAL_BLOCK;
            case 148 -> Material.RAW_COPPER;
            case 149 -> Material.COPPER_INGOT;
            case 150 -> Material.COPPER_BLOCK;
            case 151 -> Material.EXPOSED_COPPER;
            case 152 -> Material.WEATHERED_COPPER;
            case 153 -> Material.OXIDIZED_COPPER;
            case 154 -> Material.RAW_IRON;
            case 155 -> Material.IRON_INGOT;
            case 156 -> Material.IRON_BLOCK;
            case 157 -> Material.RAW_GOLD;
            case 158 -> Material.GOLD_INGOT;
            case 159 -> Material.GOLD_BLOCK;
            case 160 -> Material.LAPIS_LAZULI;
            case 161 -> Material.LAPIS_BLOCK;
            case 162 -> Material.REDSTONE;
            case 163 -> Material.REDSTONE_BLOCK;
            case 164 -> Material.DIAMOND;
            case 165 -> Material.DIAMOND_BLOCK;
            case 166 -> Material.EMERALD;
            case 167 -> Material.EMERALD_BLOCK;
            case 168 -> Material.AMETHYST_SHARD;
            case 169 -> Material.AMETHYST_BLOCK;
            case 170 -> Material.ANCIENT_DEBRIS;
            case 171 -> Material.NETHERITE_SCRAP;
            case 172 -> Material.NETHERITE_INGOT;
            case 173 -> Material.NETHERITE_BLOCK;
            case 174 -> Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
            // 175 ~ 207 네더 및 부산물
            case 175 -> Material.FLINT;
            case 176 -> Material.RESIN_CLUMP;
            case 177 -> Material.NETHERRACK;
            case 178 -> Material.NETHER_BRICKS;
            case 179 -> Material.RED_NETHER_BRICKS;
            case 180 -> Material.BASALT;
            case 181 -> Material.BLACKSTONE;
            case 182 -> Material.POLISHED_BLACKSTONE;
            case 183 -> Material.CHISELED_POLISHED_BLACKSTONE;
            case 184 -> Material.POLISHED_BLACKSTONE_BRICKS;
            case 185 -> Material.QUARTZ;
            case 186 -> Material.QUARTZ_BLOCK;
            case 187 -> Material.SOUL_SAND;
            case 188 -> Material.SOUL_SOIL;
            case 189 -> Material.MAGMA_BLOCK;
            case 190 -> Material.OBSIDIAN;
            case 191 -> Material.CRYING_OBSIDIAN;
            case 192 -> Material.GLOWSTONE;
            case 193 -> Material.GLOWSTONE_DUST;
            case 194 -> Material.SHROOMLIGHT;
            case 195 -> Material.RESPAWN_ANCHOR;
            case 196 -> Material.NETHER_WART;
            case 197 -> Material.BLAZE_ROD;
            case 198 -> Material.BLAZE_POWDER;
            case 199 -> Material.MAGMA_CREAM;
            case 200 -> Material.GHAST_TEAR;
            case 201 -> Material.PIGLIN_HEAD;
            case 202 -> Material.WITHER_SKELETON_SKULL;
            case 203 -> Material.NETHER_STAR;
            case 204 -> Material.BEACON;
            case 205 -> Material.BRICK;
            case 206 -> Material.NETHER_BRICK;
            case 207 -> Material.RESIN_BRICK;
            case 208 -> Material.CLAY_BALL;
            // 209 ~ 230 엔드 및 시련의 방
            case 209 -> Material.END_STONE;
            case 210 -> Material.END_STONE_BRICKS;
            case 211 -> Material.PURPUR_BLOCK;
            case 212 -> Material.END_ROD;
            case 213 -> Material.ENDER_PEARL;
            case 214 -> Material.ENDER_EYE;
            case 215 -> Material.SHULKER_SHELL;
            case 216 -> Material.SHULKER_BOX;
            case 217 -> Material.END_CRYSTAL;
            case 218 -> Material.DRAGON_HEAD;
            case 219 -> Material.DRAGON_BREATH;
            case 220 -> Material.ELYTRA;
            case 221 -> Material.SCULK;
            case 222 -> Material.SCULK_SENSOR;
            case 223 -> Material.SCULK_CATALYST;
            case 224 -> Material.SCULK_SHRIEKER;
            case 225 -> Material.ECHO_SHARD;
            case 226 -> Material.BREEZE_ROD;
            case 227 -> Material.WIND_CHARGE;
            case 228 -> Material.TRIAL_KEY;
            case 229 -> Material.OMINOUS_TRIAL_KEY;
            case 230 -> Material.MACE;
            // 231 ~ 254 해양/생물
            case 231 -> Material.TURTLE_EGG;
            case 232 -> Material.TURTLE_SCUTE;
            case 233 -> Material.TURTLE_HELMET;
            case 234 -> Material.SNIFFER_EGG;
            case 235 -> Material.ARMADILLO_SCUTE;
            case 236 -> Material.PRISMARINE;
            case 237 -> Material.PRISMARINE_BRICKS;
            case 238 -> Material.DARK_PRISMARINE;
            case 239 -> Material.PRISMARINE_SHARD;
            case 240 -> Material.PRISMARINE_CRYSTALS;
            case 241 -> Material.HEART_OF_THE_SEA;
            case 242 -> Material.NAUTILUS_SHELL;
            case 243 -> Material.SEA_LANTERN;
            case 244 -> Material.CONDUIT;
            case 245 -> Material.TRIDENT;
            case 246 -> Material.WATER_BUCKET;
            case 247 -> Material.COD_BUCKET;
            case 248 -> Material.SALMON_BUCKET;
            case 249 -> Material.TROPICAL_FISH_BUCKET;
            case 250 -> Material.PUFFERFISH_BUCKET;
            case 251 -> Material.AXOLOTL_BUCKET;
            case 252 -> Material.TADPOLE_BUCKET;
            case 253 -> Material.INK_SAC;
            case 254 -> Material.GLOW_INK_SAC;
            // 255 ~ 276 산호, 켈프, 개구리불
            case 255 -> Material.TUBE_CORAL_BLOCK;
            case 256 -> Material.TUBE_CORAL;
            case 257 -> Material.TUBE_CORAL_FAN;
            case 258 -> Material.BRAIN_CORAL_BLOCK;
            case 259 -> Material.BRAIN_CORAL;
            case 260 -> Material.BRAIN_CORAL_FAN;
            case 261 -> Material.BUBBLE_CORAL_BLOCK;
            case 262 -> Material.BUBBLE_CORAL;
            case 263 -> Material.BUBBLE_CORAL_FAN;
            case 264 -> Material.FIRE_CORAL_BLOCK;
            case 265 -> Material.FIRE_CORAL;
            case 266 -> Material.FIRE_CORAL_FAN;
            case 267 -> Material.HORN_CORAL_BLOCK;
            case 268 -> Material.HORN_CORAL;
            case 269 -> Material.HORN_CORAL_FAN;
            case 270 -> Material.KELP;
            case 271 -> Material.DRIED_KELP;
            case 272 -> Material.LILY_PAD;
            case 273 -> Material.SEA_PICKLE;
            case 274 -> Material.OCHRE_FROGLIGHT;
            case 275 -> Material.VERDANT_FROGLIGHT;
            case 276 -> Material.PEARLESCENT_FROGLIGHT;
            // 277 ~ 320 농작물/음식/몹전리품
            case 277 -> Material.WHEAT;
            case 278 -> Material.HAY_BLOCK;
            case 279 -> Material.CARROT;
            case 280 -> Material.GOLDEN_CARROT;
            case 281 -> Material.POTATO;
            case 282 -> Material.BAKED_POTATO;
            case 283 -> Material.POISONOUS_POTATO;
            case 284 -> Material.BEETROOT;
            case 285 -> Material.SUGAR_CANE;
            case 286 -> Material.SUGAR;
            case 287 -> Material.PUMPKIN;
            case 288 -> Material.MELON;
            case 289 -> Material.MELON_SLICE;
            case 290 -> Material.GLISTERING_MELON_SLICE;
            case 291 -> Material.APPLE;
            case 292 -> Material.GOLDEN_APPLE;
            case 293 -> Material.ENCHANTED_GOLDEN_APPLE;
            case 294 -> Material.SWEET_BERRIES;
            case 295 -> Material.GLOW_BERRIES;
            case 296 -> Material.COOKED_BEEF;
            case 297 -> Material.COOKED_PORKCHOP;
            case 298 -> Material.COOKED_MUTTON;
            case 299 -> Material.COOKED_CHICKEN;
            case 300 -> Material.COOKED_RABBIT;
            case 301 -> Material.EGG;
            case 302 -> Material.FEATHER;
            case 303 -> Material.LEATHER;
            case 304 -> Material.RABBIT_HIDE;
            case 305 -> Material.RABBIT_FOOT;
            case 306 -> Material.HONEYCOMB;
            case 307 -> Material.HONEYCOMB_BLOCK;
            case 308 -> Material.HONEY_BOTTLE;
            case 309 -> Material.HONEY_BLOCK;
            case 310 -> Material.BONE;
            case 311 -> Material.STRING;
            case 312 -> Material.SPIDER_EYE;
            case 313 -> Material.FERMENTED_SPIDER_EYE;
            case 314 -> Material.GUNPOWDER;
            case 315 -> Material.SLIME_BALL;
            case 316 -> Material.SLIME_BLOCK;
            case 317 -> Material.PHANTOM_MEMBRANE;
            case 318 -> Material.SKELETON_SKULL;
            case 319 -> Material.ZOMBIE_HEAD;
            case 320 -> Material.CREEPER_HEAD;
            // 321 ~ 362 식물, 버섯, 꽃
            case 321 -> Material.BROWN_MUSHROOM;
            case 322 -> Material.RED_MUSHROOM;
            case 323 -> resolveDynamicMaterial("SHELF_MUSHROOM", "BROWN_MUSHROOM");
            case 324 -> Material.DANDELION;
            case 325 -> Material.GOLDEN_DANDELION;
            case 326 -> Material.POPPY;
            case 327 -> Material.BLUE_ORCHID;
            case 328 -> Material.ALLIUM;
            case 329 -> Material.AZURE_BLUET;
            case 330 -> Material.OXEYE_DAISY;
            case 331 -> Material.CORNFLOWER;
            case 332 -> Material.LILY_OF_THE_VALLEY;
            case 333 -> Material.RED_TULIP;
            case 334 -> Material.ORANGE_TULIP;
            case 335 -> Material.WHITE_TULIP;
            case 336 -> Material.PINK_TULIP;
            case 337 -> Material.GLASS_PANE; // 유리판
            case 338 -> Material.TORCHFLOWER;
            case 339 -> Material.PITCHER_PLANT;
            case 340 -> Material.CACTUS_FLOWER;
            case 341 -> Material.CLOSED_EYEBLOSSOM;
            case 342 -> Material.OPEN_EYEBLOSSOM;
            case 343 -> Material.WITHER_ROSE;
            case 344 -> Material.SUNFLOWER;
            case 345 -> Material.LILAC;
            case 346 -> Material.PEONY;
            case 347 -> Material.ROSE_BUSH;
            case 348 -> Material.PINK_PETALS;
            case 349 -> Material.WILDFLOWERS;
            case 350 -> Material.LEAF_LITTER;
            case 351 -> Material.SPORE_BLOSSOM;
            case 352 -> Material.FIREFLY_BUSH;
            case 353 -> Material.SHORT_GRASS;
            case 354 -> Material.TALL_GRASS;
            case 355 -> Material.FERN;
            case 356 -> Material.LARGE_FERN;
            case 357 -> Material.SHORT_DRY_GRASS;
            case 358 -> Material.TALL_DRY_GRASS;
            case 359 -> resolveDynamicMaterial("RED_SHRUB", "DEAD_BUSH");
            case 360 -> Material.BUSH;
            case 361 -> Material.DEAD_BUSH;
            case 362 -> resolveDynamicMaterial("DRIED_GHAST", "GHAST_TEAR");
            // 363 ~ 378 염료
            case 363 -> Material.WHITE_DYE;
            case 364 -> Material.LIGHT_GRAY_DYE;
            case 365 -> Material.GRAY_DYE;
            case 366 -> Material.BLACK_DYE;
            case 367 -> Material.BROWN_DYE;
            case 368 -> Material.RED_DYE;
            case 369 -> Material.ORANGE_DYE;
            case 370 -> Material.YELLOW_DYE;
            case 371 -> Material.LIME_DYE;
            case 372 -> Material.GREEN_DYE;
            case 373 -> Material.CYAN_DYE;
            case 374 -> Material.LIGHT_BLUE_DYE;
            case 375 -> Material.BLUE_DYE;
            case 376 -> Material.PURPLE_DYE;
            case 377 -> Material.MAGENTA_DYE;
            case 378 -> Material.PINK_DYE;
            // 379 ~ 404 기타 생활/도구/모험 아이템
            case 379 -> Material.WHITE_WOOL;
            case 380 -> Material.WHITE_BANNER;
            case 381 -> Material.WHITE_BED;
            case 382 -> resolveDynamicMaterial("WHITE_CUSHION", "WHITE_CARPET");
            case 383 -> Material.CANDLE;
            case 384 -> Material.GLASS;
            case 385 -> Material.TERRACOTTA;
            case 386 -> Material.WHITE_GLAZED_TERRACOTTA;
            case 387 -> Material.WHITE_CONCRETE;
            case 388 -> Material.WHITE_CONCRETE_POWDER;
            case 389 -> Material.COMPASS;
            case 390 -> Material.RECOVERY_COMPASS;
            case 391 -> Material.CLOCK;
            case 392 -> Material.SPYGLASS;
            case 393 -> Material.FIRE_CHARGE;
            case 394 -> Material.SULFUR_CUBE_BUCKET;
            case 395 -> Material.LAVA_BUCKET;
            case 396 -> Material.POWDER_SNOW_BUCKET;
            case 397 -> Material.BUNDLE;
            case 398 -> Material.SHIELD;
            case 399 -> Material.NETHERITE_HOE;
            case 400 -> Material.GOAT_HORN;
            case 401 -> Material.MUSIC_DISC_13;
            case 402 -> Material.TOTEM_OF_UNDYING;
            case 403 -> Material.EXPERIENCE_BOTTLE;
            case 404 -> Material.TNT;
            // 405 ~ 422 갑옷 장식
            case 405 -> Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 406 -> Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 407 -> Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 408 -> Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 409 -> Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 410 -> Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 411 -> Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 412 -> Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 413 -> Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 414 -> Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 415 -> Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 416 -> Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 417 -> Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 418 -> Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 419 -> Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 420 -> Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 421 -> Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE;
            case 422 -> Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE;
            // 423 ~ 432 현수막 무늬
            case 423 -> Material.FLOWER_BANNER_PATTERN;
            case 424 -> Material.FIELD_MASONED_BANNER_PATTERN;
            case 425 -> Material.BORDURE_INDENTED_BANNER_PATTERN;
            case 426 -> Material.SKULL_BANNER_PATTERN;
            case 427 -> Material.CREEPER_BANNER_PATTERN;
            case 428 -> Material.GLOBE_BANNER_PATTERN;
            case 429 -> Material.MOJANG_BANNER_PATTERN;
            case 430 -> Material.PIGLIN_BANNER_PATTERN;
            case 431 -> Material.FLOW_BANNER_PATTERN;
            case 432 -> Material.GUSTER_BANNER_PATTERN;
            default -> Material.PAPER;
        };
    }

    private static Material resolveDynamicMaterial(String... names) {
        for (String name : names) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) return mat;
        }
        return Material.PAPER;
    }
}
