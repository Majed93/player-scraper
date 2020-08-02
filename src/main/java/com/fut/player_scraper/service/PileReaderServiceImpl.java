package com.fut.player_scraper.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fut.desktop.app.domain.AuctionInfo;
import com.fut.desktop.app.domain.AuctionResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Implementation of {@link PileReaderService}
 */
@Slf4j
@Service
public class PileReaderServiceImpl implements PileReaderService {

    private final static String START = "| ******************************** ";
    private final static String END = " ******************************** |";

    @Override
    public void totalCoinsInTradePile() throws Exception {
        AuctionResponse tradePile = convertFile("tradePile.json");

        long minValueBeforeTax = 0;
        long minValueAfterTax = 0;
        long maxValueBeforeTax = 0;
        long maxValueAfterTax = 0;

        int count = 0;
        for (AuctionInfo auction : tradePile.getAuctionInfo()) {
            minValueBeforeTax += auction.getStartingBid();
            maxValueBeforeTax += auction.getBuyNowPrice();

            minValueAfterTax += (auction.getStartingBid() * 0.95);
            maxValueAfterTax += (auction.getBuyNowPrice() * 0.95);

//            log.info(START + count + " " + " Starting price before tax: " + minValueBeforeTax + END);
//            log.info(START + count + " " + " Starting price after tax: " + minValueAfterTax + END);
//            log.info(START + count + " " + " BIN before tax: " + maxValueBeforeTax + END);
//            log.info(START + count + " " + " BIN after tax: " + maxValueAfterTax + END);
            count++;
        }

        log.info(START + " Starting price before tax: " + minValueBeforeTax + END);
        log.info(START + " Starting price after tax: " + minValueAfterTax + END);
        log.info(START + " BIN before tax: " + maxValueBeforeTax + END);
        log.info(START + " BIN after tax: " + maxValueAfterTax + END);
    }

    /**
     * Helper function to build Object from json file
     *
     * @param file File name
     * @return Object
     * @throws Exception Handle errors
     */
    private AuctionResponse convertFile(String file) throws Exception {

        File resource = new File(this.getClass().getClassLoader().getResource("json/" + file).getFile());

        log.info("File: {}", resource.getAbsolutePath());

        String json = FileUtils.readFileToString(resource, Charset.defaultCharset());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        return mapper.readValue(json, AuctionResponse.class);
    }

}
