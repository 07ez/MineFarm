package Hez.Menu;

import Hez.House.HouseData;
import Hez.House.HouseManager;
import Hez.MineFarm.Main;
import Hez.Money.MoneyManager;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BookMenu {

    private static final TextColor GOLD = TextColor.color(0xD4AF37);
    private static final TextColor GREEN = TextColor.color(0x2ECC71);
    private static final TextColor BLUE = TextColor.color(0x3498DB);
    private static final TextColor PURPLE = TextColor.color(0x9B59B6);
    private static final TextColor RED = TextColor.color(0xE74C3C);
    private static final TextColor GRAY = TextColor.color(0x7F8C8D);
    private static final TextColor DARK = TextColor.color(0x2C3E50);

    /**
     * 플레이어에게 맞춤형 책 GUI를 오픈
     */
    public static void open(Player player) {
        List<Component> pages = new ArrayList<>();

        int money = MoneyManager.getMoney(player.getUniqueId());
        HouseManager houseManager = Main.getInstance().getHouseManager();
        HouseData house = houseManager.getHouseByOwner(player.getUniqueId());
        int ownedChunks = (house != null) ? house.getChunks().size() : 0;
        int memberCount = (house != null) ? house.getMembers().size() : 0;

        // 1페이지: 메인 목차 & 홈
        pages.add(createHomePage(player.getName(), money));

        // 2페이지: 메인 퀘스트 (시즌 1: 오만의 대가)
        pages.add(createMainQuestPage());

        // 3페이지: 서브 퀘스트 (마을 의뢰 게시판)
        pages.add(createSubQuestPage());

        // 4페이지: 생태 도감 현황 (20종 목표)
        pages.add(createEncyclopediaPage());

        // 5페이지: 영지 & 집 관리 센터
        pages.add(createHousePage(ownedChunks, memberCount));

        // 6페이지: 조작 가이드 & 시스템 팁
        pages.add(createGuidePage());

        Component title = Component.text("MineFarm Menu", GOLD);
        Component author = Component.text("MineFarm", GRAY);

        Book book = Book.book(title, author, pages);
        player.openBook(book);
    }

    /**
     * [1페이지] 홈 및 메인 목차
     */
    private static Component createHomePage(String playerName, int money) {
        return Component.text()
                .append(Component.text("  🌾 MINEFARM 🌾\n", GOLD, TextDecoration.BOLD))
                .append(Component.text("───────────────────\n", GRAY))
                .append(Component.text("반갑습니다, ", NamedTextColor.BLACK))
                .append(Component.text(playerName, BLUE, TextDecoration.BOLD))
                .append(Component.text("님!\n", NamedTextColor.BLACK))
                .append(Component.text("소지금: ", NamedTextColor.BLACK))
                .append(Component.text(String.format("%,d원\n", money), GOLD, TextDecoration.BOLD))
                .append(Component.text("───────────────────\n\n", GRAY))
                // 메뉴 버튼들
                .append(createNavButton("📜 1. 메인 스토리 퀘스트", 2, "시즌 1 「오만의 대가」 메인 퀘스트 확인"))
                .append(Component.text("\n"))
                .append(createNavButton("📋 2. 마을 의뢰 게시판", 3, "마을 주민들의 일일 의뢰 확인"))
                .append(Component.text("\n"))
                .append(createNavButton("📖 3. 생태 도감 현황", 4, "도감 20종 등록 시 첫 청크권 지급!"))
                .append(Component.text("\n"))
                .append(createNavButton("🏡 4. 내 영지 / 집 관리", 5, "청크 등록 및 영지 멤버 관리"))
                .append(Component.text("\n"))
                .append(createNavButton("💡 5. 농사 & 조작 가이드", 6, "실시간 시스템 조작법 확인"))
                .append(Component.text("\n\n───────────────────\n", GRAY))
                .append(Component.text(" 👆 항목을 클릭해 이동!", GRAY))
                .build();
    }

    /**
     * [2페이지] 메인 퀘스트
     */
    private static Component createMainQuestPage() {
        return Component.text()
                .append(Component.text("  📜 메인 퀘스트\n", PURPLE, TextDecoration.BOLD))
                .append(Component.text("  시즌 1 : 오만의 대가\n", GRAY))
                .append(Component.text("───────────────────\n", GRAY))
                .append(Component.text("[ 1막: 평화로운 마을 ]\n", DARK, TextDecoration.BOLD))
                .append(Component.text("진행도: ", NamedTextColor.BLACK))
                .append(Component.text("진행 중 (과제 보조)\n\n", GREEN, TextDecoration.BOLD))
                .append(Component.text("연구소장 헬름의 생태 조사를 돕기 위해 작물 샘플을 모으세요.\n\n", NamedTextColor.BLACK))
                .append(Component.text("[ 🔍 퀘스트 상세 정보 ]\n", BLUE, TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(Component.text(
                                "§e[요구 재료]\n§f- 밀 10개 & 감자 10개 납품\n§f신선한 작물 샘플을 연구소에 전달하세요!\n\n§a[완료 보상]\n§f- 1,500원 & 경험치 300"))))
                .append(Component.text("[ 🎁 보상 수령 시도 ]\n\n", GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/money"))
                        .hoverEvent(HoverEvent.showText(Component.text("§a클릭 시 퀘스트 달성 여부를 확인하고 보상을 수령합니다."))))
                .append(Component.text("🔒 2막: 불사의 몸 연구 (잠김)\n", GRAY))
                .append(Component.text("───────────────────\n", GRAY))
                .append(createBackButton())
                .build();
    }

    /**
     * [3페이지] 서브 퀘스트 (의뢰)
     */
    private static Component createSubQuestPage() {
        return Component.text()
                .append(Component.text("  📋 마을 의뢰 게시판\n", GOLD, TextDecoration.BOLD))
                .append(Component.text("───────────────────\n", GRAY))
                .append(Component.text("1. 낚시꾼 밥의 의뢰\n", DARK, TextDecoration.BOLD))
                .append(Component.text("  - 목표: 바다 대구 5마리\n", NamedTextColor.BLACK))
                .append(Component.text("  - 보상: 800원 & 낚싯대\n\n", GRAY))
                .append(Component.text("2. 농부 잭의 감자 수확\n", DARK, TextDecoration.BOLD))
                .append(Component.text("  - 목표: 감자 20개 수확\n", NamedTextColor.BLACK))
                .append(Component.text("  - 보상: 1,200원\n\n", GRAY))
                .append(Component.text("3. 대장장이의 광석 조달\n", DARK, TextDecoration.BOLD))
                .append(Component.text("  - 목표: 야생 철광석 10개\n", NamedTextColor.BLACK))
                .append(Component.text("  - 보상: 2,000원\n\n", GRAY))
                .append(Component.text("───────────────────\n", GRAY))
                .append(createBackButton())
                .build();
    }

    /**
     * [4페이지] 생태 도감 현황
     */
    private static Component createEncyclopediaPage() {
        return Component.text()
                .append(Component.text("  📖 생태 도감 현황\n", GREEN, TextDecoration.BOLD))
                .append(Component.text("───────────────────\n", GRAY))
                .append(Component.text("등록 현황: ", NamedTextColor.BLACK))
                .append(Component.text("3", GREEN, TextDecoration.BOLD))
                .append(Component.text(" / ", GRAY))
                .append(Component.text("20 종\n", RED, TextDecoration.BOLD))
                .append(Component.text("★ 20종 등록 시 첫 청크권 지급!\n\n", GOLD))
                .append(Component.text("🌾 작물: ", DARK, TextDecoration.BOLD))
                .append(createItemBadge("밀", true, "농경지에서 수확 가능"))
                .append(Component.text(" "))
                .append(createItemBadge("감자", true, "10초 후 수확 가능 (테스트)"))
                .append(Component.text(" "))
                .append(createItemBadge("당근", false, "아직 미등록 작물"))
                .append(Component.text("\n\n"))
                .append(Component.text("🐟 어종: ", DARK, TextDecoration.BOLD))
                .append(createItemBadge("대구", true, "바다/호수에서 낚시"))
                .append(Component.text(" "))
                .append(createItemBadge("연어", false, "강 바이옴에서 낚시"))
                .append(Component.text(" "))
                .append(createItemBadge("블롭피쉬", false, "심해 바다 낚시"))
                .append(Component.text("\n\n"))
                .append(Component.text("🍳 요리: ", DARK, TextDecoration.BOLD))
                .append(createItemBadge("생선구이", false, "생선 + 밀가루 조합"))
                .append(Component.text(" "))
                .append(createItemBadge("회", false, "물고기 손질 요리"))
                .append(Component.text("\n───────────────────\n", GRAY))
                .append(createBackButton())
                .build();
    }

    /**
     * [5페이지] 영지 관리
     */
    private static Component createHousePage(int ownedChunks, int memberCount) {
        return Component.text()
                .append(Component.text("  🏡 영지 관리 센터\n", BLUE, TextDecoration.BOLD))
                .append(Component.text("───────────────────\n", GRAY))
                .append(Component.text("보유 청크: ", NamedTextColor.BLACK))
                .append(Component.text(ownedChunks + "개\n", BLUE, TextDecoration.BOLD))
                .append(Component.text("영지 멤버: ", NamedTextColor.BLACK))
                .append(Component.text(memberCount + "명\n\n", GREEN, TextDecoration.BOLD))
                // 원클릭 명령어 버튼
                .append(createCommandButton("📍 현재 청크 소유자 확인", "/house who", "현재 서 있는 청크가 누구의 땅인지 확인합니다."))
                .append(Component.text("\n\n"))
                .append(createCommandButton("🚩 현재 청크 내 땅으로 등록", "/house claim", "현재 청크를 내 영지로 등록합니다."))
                .append(Component.text("\n\n"))
                .append(createCommandButton("👥 영지 멤버 목록 보기", "/house member list", "등록된 멤버 목록을 확인합니다."))
                .append(Component.text("\n\n───────────────────\n", GRAY))
                .append(createBackButton())
                .build();
    }

    /**
     * [6페이지] 조작 가이드 & 시스템 팁
     */
    private static Component createGuidePage() {
        return Component.text()
                .append(Component.text("  💡 조작 가이드 & 팁\n", GOLD, TextDecoration.BOLD))
                .append(Component.text("───────────────────\n", GRAY))
                .append(Component.text("1. 실시간 농사 시스템\n", DARK, TextDecoration.BOLD))
                .append(Component.text("- 작물에 ", NamedTextColor.BLACK))
                .append(Component.text("웅크리기+우클릭", BLUE, TextDecoration.BOLD))
                .append(Component.text(" 시 머리 위에 실시간 타이머 표시!\n", NamedTextColor.BLACK))
                .append(Component.text("- 시선(크로스헤어) 이탈 시 자동 해제.\n\n", GRAY))
                .append(Component.text("2. 상점 빠른 거래\n", DARK, TextDecoration.BOLD))
                .append(Component.text("- 상점 디스플레이 조준 후:\n", NamedTextColor.BLACK))
                .append(Component.text("  우클릭: 1개 판매\n", GRAY))
                .append(Component.text("  Shift+우클릭: 전량 판매\n", GRAY))
                .append(Component.text("  좌클릭: 1개 구매\n", GRAY))
                .append(Component.text("  Shift+좌클릭: 1세트 구매\n\n", GRAY))
                .append(Component.text("───────────────────\n", GRAY))
                .append(createBackButton())
                .build();
    }

    // --- 헬퍼 메서드 ---

    private static Component createNavButton(String label, int pageNumber, String hoverTooltip) {
        return Component.text("[" + label + "]", BLUE)
                .clickEvent(ClickEvent.changePage(pageNumber))
                .hoverEvent(HoverEvent.showText(Component.text("§b" + hoverTooltip + "\n§7클릭하여 이동합니다.")));
    }

    private static Component createCommandButton(String label, String command, String hoverTooltip) {
        return Component.text("[" + label + "]", GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text("§a" + hoverTooltip + "\n§7클릭 시 실행: " + command)));
    }

    private static Component createItemBadge(String name, boolean registered, String tooltip) {
        TextColor color = registered ? GREEN : GRAY;
        String prefix = registered ? "§a✓ " : "§7✗ ";
        return Component.text("[" + (registered ? "✓" : "✗") + name + "]", color)
                .hoverEvent(HoverEvent.showText(Component.text(prefix + name + "\n§f" + tooltip)));
    }

    private static Component createBackButton() {
        return Component.text("↩ [ 메인 목차로 돌아가기 ]", RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.changePage(1))
                .hoverEvent(HoverEvent.showText(Component.text("§c메인 목차(1페이지)로 즉시 돌아갑니다.")));
    }
}
