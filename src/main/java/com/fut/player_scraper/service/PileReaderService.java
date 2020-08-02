package com.fut.player_scraper.service;

/**
 * Pile Reader service.
 */
public interface PileReaderService {

    /**
     * Read trade pile JSON file and print out the results of total coins.
     */
    void totalCoinsInTradePile() throws Exception;
}
