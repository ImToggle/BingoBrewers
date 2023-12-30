package com.github.indigopolecat.bingobrewers;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.Objects;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;


public class auctionAPI {

    public static ArrayList<Double> neulbinSearch(ArrayList<String> items) {
        CompletableFuture<HashMap<String, Double>> lbinMap = new CompletableFuture<>();
        try {
            Thread apiRequest = new Thread(() -> {
               try {
                   URL apiURL = new URL("https://moulberry.codes/lowestbin.json.gz");

                   HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
                   connection.setRequestMethod("GET");
                   connection.setRequestProperty("Accept-Encoding", "gzip");
                   connection.connect();

                   InputStream inputStream = connection.getInputStream();
                   GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                   ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                   byte[] buffer = new byte[1024];
                   int len;
                   while ((len = gzipInputStream.read(buffer)) > 0) {
                       byteArrayOutputStream.write(buffer, 0, len);
                   }

                   byte[] byteArray = byteArrayOutputStream.toByteArray();

                   String json = new String(byteArray, StandardCharsets.UTF_8);
                   Type type = new TypeToken<HashMap<String, Double>>() {}.getType();
                   HashMap<String, Double> lbin = new Gson().fromJson(json, type);

                   lbinMap.complete(lbin);

               } catch (IOException e) {
                   System.out.println("Error: " + e);
               }
            });

            apiRequest.start();

            ArrayList<Item> itemList = new ArrayList<>();

            for (String s : items) {
                Item item = new Item(s);
                itemList.add(item);
            }

            ArrayList<Double> costs = new ArrayList<>();
            for (String item : items) {

                try {
                    item = item.replace(" ", "_").toUpperCase();
                    costs.add(lbinMap.get().get(item));
                } catch (Exception e) {
                    System.out.println("Item not found in auction house: " + item);
                }
            }
            return costs;
        } catch (Exception e) {
            System.out.println("Error: " + e);
            return null;
        }
    }



    // This method is unused. keeping incase we need to do it on our own server in the future. Combined with Item.java it fetches lbin of an array of display names input.

    public static ArrayList<Double> auctionAPISearch(ArrayList<String> items) {
        String apiURL = "https://api.hypixel.net/skyblock/auctions";

        // query api plus some anti error stuff
        String json = Objects.requireNonNull(queryAPI(apiURL)).toString();

        ArrayList<Item> itemList = new ArrayList<>();
        for (String s : items) {
            Item item = new Item(s);

            itemList.add(item);
        }
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

        int totalPages = jsonObject.get("totalPages").getAsInt();
        for (int i = 0; i < totalPages; i++) {

            // This crashes if a page is removed while this is running

            String auctionPage = Objects.requireNonNull(queryAPI("https://api.hypixel.net/skyblock/auctions?page=" + i)).toString();
            JsonObject auctionJSON = new Gson().fromJson(auctionPage, JsonObject.class);
            JsonArray auctions = auctionJSON.get("auctions").getAsJsonArray();
            System.out.println("page: " + auctionJSON.get("page").getAsInt());

            for (int j = 0; j < auctions.size(); j++) {
                JsonObject auction = auctions.get(j).getAsJsonObject();
                String item = auction.get("item_name").getAsString();
                System.out.print(item + ", ");

                if (items.contains(item)) {
                    System.out.println("Found item!");

                    if (auction.get("bin").getAsBoolean()) {
                        System.out.println("Item is BIN!");
                        int price = auction.get("starting_bid").getAsInt();
                        Item itemObject = getItemByName(itemList, item);
                        if (itemObject != null) {
                            System.out.println(itemObject.getName());
                        }
                        if (itemObject != null) {
                            itemObject.addCost(price);
                        }


                    }
                }
            }
            System.out.println("done with page " + i);
        }
        ArrayList<Double> costs = new ArrayList<>();
        for (Item item : itemList) {
            costs.add(item.getLowestCost());
        }
        return costs;
    }

    private static Item getItemByName(ArrayList<Item> itemList, String itemName) {
        for (Item item : itemList) {
            if (item.getName().equals(itemName)) {
                return item; // Return the found item
            }
        }
        return null; // Return null if the item is not found
    }


    private static StringBuffer queryAPI(String apiURL) {
        try {
            URL url = new URL(apiURL);

            HttpURLConnection api = (HttpURLConnection) url.openConnection();
            api.setRequestMethod("GET");
            int responseCode = api.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(api.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response;
            } else {
                System.out.println("API connection failed!");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
        return null;
    }
}
