package com.fut.player_scraper;

import com.fut.desktop.app.domain.AuctionInfo;
import com.fut.desktop.app.extensions.LongExtensions;
import com.fut.desktop.app.restObjects.Player;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.*;

@Slf4j
public final class GoldIFPlayers {

    private static JsonArray players;

    public static void getGoldIFs(JsonArray players) throws Exception {
        GoldIFPlayers.players = players;

        String league = "GoldIF";

        // ************************ Search conditions **************************
        // **    Players rate 82 - 99                               ************
        // **    Xbox price between 13K and 35K                     ************
        // **    IF Gold version and ONLY IF revision (first IF)    ************
        // **    Sort by xbox price in descending order             ************
        // *********************************************************************

//        String leagueScrapeUrl = "https://www.futbin.com/18/players?page=1&player_rating=81-99&xbox_price=13000-35000&version=if_gold&sort=xbox_price&order=desc";
        String leagueScrapeUrl = "https://www.futbin.com/18/players?page=1&player_rating=81-99&xbox_price=13000-35000&version=if_gold&sort=xbox_price&order=desc&revision=IF";

        league = league.replaceAll(" ", "");

        String fileName = FileUtils.DATA_DIR + league + FileUtils.PLAYERS_JSON;
        log.info("File name: " + fileName);
        File file = new File(fileName);

        if (!file.exists()) {
            Document listOfPlayers = Jsoup.connect(leagueScrapeUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                    .maxBodySize(0)
                    .get();
            Elements playerTable = listOfPlayers.select("table#repTb");

            //Build up list of pages we'll need.
            List<Elements> pages = getPages(playerTable);
            // new ArrayList<>();
            //            pages.add(playerTable);
            //Scrape each players and SAVE the player objects here.
            getEachPlayers(pages, league);
        } else {
            log.info("Skipping because already exists.");
        }
    }


    /**
     * Get the number of pages to scrape.
     *
     * @param playerTable First page
     * @return list of tables for each league.
     */
    private static List<Elements> getPages(Elements playerTable) throws Exception {
        List<Elements> urls = new LinkedList<>(); // It's really a list of the table elements from each page.

        Elements nextButton = playerTable.parents().select("a#next");
        // NOTE: This code is out of date, copy from the application if you still want to use this method
        if (nextButton.size() > 0) {
            String pageUrl = FileUtils.futBin + nextButton.attr("href");
            pageUrl = pageUrl.replace("///", "/");
            log.info("************* {}", pageUrl);
            urls.add(playerTable);
            Thread.sleep(FileUtils.TIMEOUT * 2);
            //Go to next page now.
            Document listOfPlayers = Jsoup.connect(pageUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                    .maxBodySize(0)
                    .get();

            Elements elements = listOfPlayers.select("table#repTb").select("tr[class*=player_tr_]");

            if (elements != null && !elements.isEmpty() && !elements.text().contains("No Results")) {
                urls.addAll(getPages(elements));
            } else {
                return urls;
            }
        }
        // If only one page.
        urls.add(playerTable);
        return urls;
    }

    /**
     * Get each player url NOTE: only for silvers
     *
     * @param pages each page
     */
    private static void getEachPlayers(List<Elements> pages, String leagueName) {
        Set<Player> playersList = new LinkedHashSet<>();
        boolean stop = false;
        for (Elements playerTable : pages) {
            if (!stop) {
                for (Element playerRow : playerTable.select("tr[class*=player_tr_]")) {
                    if (!stop) {
                        //Need to get assetId first.
                        String[] href = playerRow.select("img[class*=player_img]").attr("src").split("/");
                        String strAssetId = href[href.length - 1].split("\\.")[0];

                        // **** THIS DOES NOT WORK
                        Long assetId = LongExtensions.CalculateBaseId(Long.parseLong(strAssetId.replaceAll("[^\\d.]", "")));

                        if (!listContainsPlayer(playersList, assetId)) {


                            //Check if IF item
                            //Also check if LAST item is less than 1.1k
                            String lastPlayerPrice = playerRow.select("span[class=xb1_color]").text();
                            Integer lastPlayerIntPrice;

                            //Check for price on LAST item
                            lastPlayerPrice = lastPlayerPrice.replace("K", "");
                            Double unmodified = 0.0;

                            try {
                                unmodified = Double.valueOf(lastPlayerPrice);
                            } catch (Exception ex) {
                                log.error("Player: {}", assetId);
                                log.error("ermm.. {}", ex);
                            }
                            lastPlayerIntPrice = (int) (unmodified * 1000);

                            if (lastPlayerIntPrice > FileUtils.lowestPrice) {
                                //SAVE PLAYER
                                //Need to construct player data.
                                Player player = new Player();


                                JsonObject jsonPlayer = null;
                                for (JsonElement jsonElement : players) {
                                    JsonObject singlePlayerObject = jsonElement.getAsJsonObject();

                                    if (singlePlayerObject.get("id").getAsLong() == assetId) {
                                        jsonPlayer = singlePlayerObject;
                                    }
                                }

                                if (jsonPlayer == null) {
                                    log.error("Player {} not found!!", assetId);
                                } else {
                                    String[] clubArray = playerRow.select("span[class=players_club_nation]").select("a").get(0).select("img").attr("src").split("/");
                                    String[] leagueArray = playerRow.select("span[class=players_club_nation]").select("a").get(2).select("img").attr("src").split("/");

                                    Integer clubId = Integer.valueOf(clubArray[clubArray.length - 1].split("\\.")[0]);
                                    Integer leagueId = Integer.valueOf(leagueArray[leagueArray.length - 1].split("\\.")[0]);

                                    Integer rating = Integer.valueOf(playerRow.select("span[class*=rating]").text().trim());


                                    JsonElement firstNameElement = jsonPlayer.get("f");
                                    JsonElement lastNameElement = jsonPlayer.get("l");
                                    JsonElement commonNameElement = jsonPlayer.get("c");

                                    player.setFirstName(firstNameElement == null ? "" : firstNameElement.getAsString());
                                    player.setLastName(lastNameElement == null ? "" : lastNameElement.getAsString());
                                    player.setCommonName(commonNameElement == null ? "" : commonNameElement.getAsString());
                                    player.setRating(rating);
                                    player.setAssetId(jsonPlayer.get("id").getAsLong()); //from img parsing
                                    player.setPosition(playerRow.select("td").get(2).text());
                                    player.setLowestBin(AuctionInfo.roundToNearest(lastPlayerIntPrice)); //TODO:
                                    player.setSearchPrice(FileUtils.lowestPrice); //TODO:
                                    player.setMaxListPrice(0L);
                                    player.setMinListPrice(0L);
                                    player.setNation(jsonPlayer.get("n").getAsInt());
                                    player.setClub(clubId);
                                    player.setLeague(leagueId);
                                    player.setBidAmount(0L);
                                    player.setPriceSet(false);

                                    //Add to respective list so it's sorted.
                                    playersList.add(player);
                                }
                            } else {
                                stop = true;
                            }
                        }
                    }
                }
            }
        }
        FileUtils.writeFile(playersList, leagueName);
        log.info("Done league: " + leagueName);
    }

    /**
     * check if list already contains the player
     *
     * @param playersList list to check
     * @param assetId     asset id of player to check
     * @return
     */
    private static boolean listContainsPlayer(Set<Player> playersList, Long assetId) {
        for (Player player : playersList) {
            if (Objects.equals(player.getAssetId(), assetId)) {
                return true;
            }
        }
        return false;
    }
}
