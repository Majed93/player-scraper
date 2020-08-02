package com.fut.player_scraper;

import com.fut.desktop.app.constants.Resources;
import com.fut.desktop.app.domain.AuctionInfo;
import com.fut.desktop.app.restObjects.Player;
import com.fut.player_scraper.service.PileReaderService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class PlayerScraperApplication {

    private static Logger log = LoggerFactory.getLogger(PlayerScraperApplication.class);

    private static JsonArray players;

    private static String platformCookieName = "platform";
    private static String platformCookieValue = "xone";

    @Autowired
    private PileReaderService pileReaderService;

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(PlayerScraperApplication.class, args);
        PlayerScraperApplication app = context.getBean(PlayerScraperApplication.class);
        app.start(args);
//         Get players
//        getPlayers();

        // Get TOTW
//        getTOTW();

        System.exit(1);
    }

    private void start(String[] args) throws Exception {
        this.pileReaderService.totalCoinsInTradePile();
    }

    /**
     * Create a list of players. Will not amend.
     */
    private static void getTOTW() throws Exception {
        players = FileUtils.getAllPlayers();

        // NOTE: *************************************************************************
        // NOTE: ************** Change this to be the current TOTW ***********************
        // NOTE: *************************************************************************
        String totw = "TOTW35";
        String urlToScrape = "https://www.futbin.com/" + Resources.FUT_YEAR + "/totw/" + totw;
        Document document = Jsoup.connect(urlToScrape)
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                .maxBodySize(0)
                .cookie(platformCookieName, platformCookieValue)
                .get();

        Elements card = document.select(".card");

        List<Player> players = new ArrayList<>();

        card.forEach(element -> {
            String playerPageURL = "https://www.futbin.com" + element.select("a").attr("href");
            Document playerPage = null;
            try {
                playerPage = Jsoup.connect(playerPageURL)
                        .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                        .maxBodySize(0)
                        .cookie(platformCookieName, platformCookieValue)
                        .get();
            } catch (IOException e) {
                log.error("Unable to get player page {}", playerPageURL);
                e.printStackTrace();
            }

            Player player = new Player();
            // asset id
            String assetId = element.select("div[class*=compare-button]").attr("data-baseid");
            JsonObject jsonPlayer = getPlayerFromAssetId(assetId);

            if (playerPage != null && jsonPlayer != null) {
                JsonElement firstNameElement = jsonPlayer.get("f");
                JsonElement lastNameElement = jsonPlayer.get("l");
                JsonElement commonNameElement = jsonPlayer.get("c");

                // rating
                String rating = element.select("div[class*=pcdisplay-rat]").text().trim();

                // position
                String position = element.select("div[class*=pcdisplay-pos]").text().trim();

                // nation
                String[] nationArray = element.select("img#player_nation").attr("src").split("/");
                Integer nationId = Integer.valueOf(nationArray[nationArray.length - 1].split("\\.")[0]);

                // club
                String[] clubArray = element.select("img#player_club").attr("src").split("/");
                Integer clubId = Integer.valueOf(clubArray[clubArray.length - 1].split("\\.")[0]);

                // league
                String[] leagueArray = playerPage.select("img[alt=league]").attr("src").split("/");
                Integer leagueId = Integer.valueOf(leagueArray[leagueArray.length - 1].split("\\.")[0]);

                player.setFirstName(firstNameElement == null ? "" : firstNameElement.getAsString());
                player.setLastName(lastNameElement == null ? "" : lastNameElement.getAsString());
                player.setCommonName(commonNameElement == null ? "" : commonNameElement.getAsString());
                player.setRating(Integer.valueOf(rating));
                player.setAssetId(jsonPlayer.get("id").getAsLong());
                player.setPosition(position);
                player.setLowestBin(0L); //TODO:
                player.setSearchPrice(0L); //TODO:
                player.setMaxListPrice(0L);
                player.setMinListPrice(0L);
                player.setNation(nationId);
                player.setClub(clubId);
                player.setLeague(leagueId);
                player.setBidAmount(0L);
                player.setPriceSet(false);

                players.add(player);
            }
        });

        // Sort the list with lowest rating first.

        players.sort(Comparator.comparingInt(Player::getRating));

        FileUtils.writeFile(new LinkedHashSet<>(players), totw);
        log.info("Done TOTW: " + totw);
    }

    /**
     * Get the players
     *
     * @throws Exception
     */
    private static void getPlayers() throws Exception {

        players = FileUtils.getAllPlayers();

        // Do gold IFs
        // GoldIFPlayers.getGoldIFs(players);


      /*  for (JsonElement o : players) {
            JsonObject singlePlayerObject = o.getAsJsonObject();
            singlePlayerObject.get("c"); // Luis Garcia
            singlePlayerObject.get("cf"); // Luis
            singlePlayerObject.get("l"); // Garcia
            singlePlayerObject.get("n"); // 45 // nation id
            singlePlayerObject.get("r"); // 70 // rating
            singlePlayerObject.get("id"); // 16 //asset id
        }*/

        //Scrape futbin first
        // TODO: put back in
        Map<String, String> leagueUrls = /*new HashMap<>();*/getLeagueUrls();
//        leagueUrls.put("Premier League", "https://www.futbin.com/19/players?page=1&league=13");
//        leagueUrls.put("LIGA Bancomer MX", "https://www.futbin.com/19/players?page=1&sort=xbox_price&order=desc&league=341&version=silver");
        // leagueUrls.put("Gold IFs", "https://www.futbin.com/19/players?page=1&player_rating=82-99&xbox_price=13000-35000&version=if_gold&sort=xbox_price&order=desc");
        // https://www.futbin.com/19/players?page=1&xbox_price=13000-35000&version=if_gold

        // Go to each page and start processing players
        // example url https://www.futbin.com/19/players?page=1&version=silver&league=13&sort=xbox_price&order=desc

        //*** SILVERS **************************************

        for (String league : leagueUrls.keySet()) {
            String leagueScrapeUrl = leagueUrls.get(league) + "&version=silver&sort=xbox_price&order=desc";
            // + "&player_rating=82-99&xbox_price=13000-35000&version=if_gold&sort=xbox_price&order=desc";
            // + "&version=silver&sort=xbox_price&order=desc";

            league = league.replaceAll(" ", "");

            String fileName = FileUtils.DATA_DIR + league + FileUtils.PLAYERS_JSON;
            log.info("File name: " + fileName);
            File file = new File(fileName);
            if (!file.exists()) {

                log.info("Starting league: " + leagueScrapeUrl);

                Document listOfPlayers = Jsoup.connect(leagueScrapeUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                        .maxBodySize(0)
                        .cookie(platformCookieName, platformCookieValue)
                        .get();
                Elements playerTable = listOfPlayers.select("table#repTb");

                //Build up list of pages we'll need.
                List<Elements> pages = getPages(playerTable);

                //Scrape each players and SAVE the player objects here.
                getEachPlayers(pages, league);
            } else {
                log.info("Skipping because already exists.");
            }
        }

        log.info("Done creating json");

    }

    /**
     * Get each player url NOTE: only for silvers
     *
     * @param pages each page
     */
    private static void getEachPlayers(List<Elements> pages, String leagueName) throws Exception {
        Set<Player> playersList = new LinkedHashSet<>();

        Set<Player> GKList = new LinkedHashSet<>();
        Set<Player> LBList = new LinkedHashSet<>();
        Set<Player> RBList = new LinkedHashSet<>();
        Set<Player> CBList = new LinkedHashSet<>();
        Set<Player> CDMList = new LinkedHashSet<>();
        Set<Player> CMList = new LinkedHashSet<>();
        Set<Player> CAMList = new LinkedHashSet<>();
        Set<Player> LMList = new LinkedHashSet<>();
        Set<Player> LWList = new LinkedHashSet<>();
        Set<Player> RMList = new LinkedHashSet<>();
        Set<Player> RWList = new LinkedHashSet<>();
        Set<Player> CFList = new LinkedHashSet<>();
        Set<Player> STList = new LinkedHashSet<>();

        boolean stop = false;
        for (Elements playerTable : pages) {
            if (!stop) {
                for (Element playerRow : playerTable.select("tr[class*=player_tr_]")) {
                    if (!stop) {

                        // NOTE: Check here if items are special since i only want normal ones *****
                        //Check if IF item
                        Elements informItems = playerRow.select("span[class*=form rating if]");
                        Elements swapItems = playerRow.select("span[class*=form rating swap_deals_1]");

                        if (informItems.size() < 1 && swapItems.size() < 1) {
                            //Also check if LAST item is less than 1.1k
                            String lastPlayerPrice = playerRow.select("span[class*=xb1_color]").text();
                            Integer lastPlayerIntPrice;

                            //Check for price on LAST item
                            if (lastPlayerPrice.contains("K")) {
                                lastPlayerPrice = lastPlayerPrice.replace("K", "");
                                Double unmodified = Double.valueOf(lastPlayerPrice);
                                lastPlayerIntPrice = (int) (unmodified * 1000);

                            } else {
                                lastPlayerIntPrice = Integer.valueOf(lastPlayerPrice);
                            }

                            if (lastPlayerIntPrice > FileUtils.lowestPrice) {
                                //SAVE PLAYER
                                //Need to construct player data.
                                Player player = new Player();

                                //Need to get assetId first.
                                String[] href = playerRow.select("img[class*=player_img]").attr("data-original").split("/");
                                String assetId = href[href.length - 1].split("\\.")[0];

                                JsonObject jsonPlayer = null;
                                jsonPlayer = getPlayerFromAssetId(assetId);

                                if (jsonPlayer == null) {
                                    System.out.println("Player " + assetId + " not found!");
                                } else {

                                    String[] clubArray = playerRow.select("span[class=players_club_nation]").select("a").get(0).select("img").attr("src").split("/");
                                    String[] nationArray = playerRow.select("span[class=players_club_nation]").select("a").get(1).select("img").attr("src").split("/");
                                    String[] leagueArray = playerRow.select("span[class=players_club_nation]").select("a").get(2).select("img").attr("src").split("/");

                                    Integer clubId = Integer.valueOf(clubArray[clubArray.length - 1].split("\\.")[0]);
                                    Integer nationId = Integer.valueOf(nationArray[nationArray.length - 1].split("\\.")[0]);
                                    Integer leagueId = Integer.valueOf(leagueArray[leagueArray.length - 1].split("\\.")[0]);

                                    JsonElement firstNameElement = jsonPlayer.get("f");
                                    JsonElement lastNameElement = jsonPlayer.get("l");
                                    JsonElement commonNameElement = jsonPlayer.get("c");

                                    player.setFirstName(firstNameElement == null ? "" : firstNameElement.getAsString());
                                    player.setLastName(lastNameElement == null ? "" : lastNameElement.getAsString());
                                    player.setCommonName(commonNameElement == null ? "" : commonNameElement.getAsString());
                                    player.setRating(jsonPlayer.get("r").getAsInt());
                                    player.setAssetId(jsonPlayer.get("id").getAsLong()); //from img parsing
                                    player.setPosition(playerRow.select("td").get(2).text());
                                    player.setLowestBin(AuctionInfo.roundToNearest(lastPlayerIntPrice)); //TODO:
                                    player.setSearchPrice(FileUtils.lowestPrice); //TODO:
                                    player.setMaxListPrice(0L);
                                    player.setMinListPrice(0L);
//                                    player.setNation(0);
                                    // Nation no longer set in the json from EA.
                                    // Get nation from the page


                                    player.setNation(nationId);
                                    player.setClub(clubId);
                                    player.setLeague(leagueId);
                                    player.setBidAmount(0L);
                                    player.setPriceSet(false);

                                    //Add to respective list so it's sorted.

                                    String pp = player.getPosition();

                                    switch (pp.toUpperCase()) {
                                        case "GK":
                                            GKList.add(player);
                                            break;
                                        case "LB":
                                            LBList.add(player);
                                            break;
                                        case "LWB":
                                            LBList.add(player);
                                            break;
                                        case "RB":
                                            RBList.add(player);
                                            break;
                                        case "RWB":
                                            RBList.add(player);
                                            break;
                                        case "CB":
                                            CBList.add(player);
                                            break;
                                        case "CDM":
                                            CDMList.add(player);
                                            break;
                                        case "CM":
                                            CMList.add(player);
                                            break;
                                        case "CAM":
                                            CAMList.add(player);
                                            break;
                                        case "LM":
                                            LMList.add(player);
                                            break;
                                        case "LW":
                                            LWList.add(player);
                                            break;
                                        case "RM":
                                            RMList.add(player);
                                            break;
                                        case "RW":
                                            RWList.add(player);
                                            break;
                                        case "CF":
                                            CFList.add(player);
                                            break;
                                        default: /* Default add as striker */
                                            STList.add(player);
                                            break;
                                    }
                                }
                            } else {
                                stop = true;
                            }
                        }
                    }
                }
            }
        }

        //Add all lists
        playersList.addAll(GKList);
        playersList.addAll(LBList);
        playersList.addAll(RBList);
        playersList.addAll(CBList);
        playersList.addAll(CDMList);
        playersList.addAll(CMList);
        playersList.addAll(CAMList);
        playersList.addAll(LMList);
        playersList.addAll(LWList);
        playersList.addAll(RMList);
        playersList.addAll(RWList);
        playersList.addAll(CFList);
        playersList.addAll(STList);
        FileUtils.writeFile(playersList, leagueName);
        log.info("Done league: " + leagueName);
    }

    /**
     * Get the number of pages to scrape.
     *
     * @param playerTable First page
     * @return list of tables for each league.
     */
    private static List<Elements> getPages(Elements playerTable) throws Exception {
        List<Elements> urls = new LinkedList<>();

        Elements nextButton = playerTable.parents().select(".page-item > a").select("span.sr-only");

        if (nextButton.size() == 1) {

            Element nextButtonWithUrl = playerTable.parents().select(".page-item > a").get(playerTable.parents().select(".page-item > a").size() - 1);

            if (!nextButtonWithUrl.parent().hasClass("active")) {
                String pageUrl = FileUtils.futBin + nextButtonWithUrl.attr("href");
                pageUrl = pageUrl.replace("///", "/");

                urls.add(playerTable);
                Thread.sleep(FileUtils.TIMEOUT * 2);
                //Go to next page now.
                Document listOfPlayers = Jsoup.connect(pageUrl)
//                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//                    .header("accept-encoding", "gzip, deflate, br")
//                    .header("accept-language", "en-US,en;q=0.5")
//                    .header("TE", "Trailers")
//                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                        .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                        .maxBodySize(0)
                        .cookie(platformCookieName, platformCookieValue)
                        .get();
                urls.addAll(getPages(listOfPlayers.select("table#repTb")));
            }
        }

        return urls;
    }

    /**
     * Return a list of league urls. This will get ALL the leagues
     *
     * @return list of league urls
     */
    private static Map<String, String> getLeagueUrls() throws Exception {
        Map<String, String> urls = new HashMap<>();
        String url = FileUtils.futBin + "19/players";

        Document document = Jsoup.connect(url)
                //.header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                //.header("accept-encoding", "gzip, deflate, br")
                //.header("accept=language", "en-US,en;q=0.8")
//                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                .maxBodySize(0)
                .cookie(platformCookieName, platformCookieValue)
                .get();
        Thread.sleep((long) (FileUtils.TIMEOUT * 0.5));
        Elements liElements = document.select("li[class=dropdown dropdown-submenu leagues_sub]").select("li");

        for (Element element : liElements) {
            String href = element.select("a").attr("href");

            //Ignore some specific leagues
            if (!foundLeague(href) && href.contains("league=")) {
                href = href.replaceAll("/19/player", "19/player");
                urls.put(element.text(), FileUtils.futBin + href);
            }
        }
        return urls;
    }

    /**
     * Checks if href has the league id to omit.
     *
     * @param href Link to check
     * @return True if found otherwise false
     */
    private static boolean foundLeague(String href) {
        if (href.contains("17")) {
            return true;
        } else if (href.contains("332")) {
            return true;
        } else if (href.contains("65")) {
            return true;
        } else if (href.contains("2118")) {
            return true;
        } else if (href.contains("32")) {
            return true;
        } else if (href.contains("319")) {
            return true;
        } else if (href.contains("61")) {
            return true;
        } else if (href.contains("66")) {
            return true;
        } else if (href.contains("41")) {
            return true;
        } else if (href.contains("322")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get player from the EA json file
     *
     * @param assetId The asset id to query
     * @return The found player, otherwise null
     */
    private static JsonObject getPlayerFromAssetId(String assetId) {
        for (JsonElement jsonElement : players) {
            JsonObject singlePlayerObject = jsonElement.getAsJsonObject();

            if (singlePlayerObject.get("id").getAsString().equals(assetId)) {
                return singlePlayerObject;
            }
        }
        return null;
    }
}
