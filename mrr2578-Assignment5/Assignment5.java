import java.util.*;
import java.io.*;

public class Assignment5 {

   static Room[] roomsArray;      
   static int[] priceVector;
   static ArrayList<Bid> bids = new ArrayList<Bid>();
   static LinkedHashMap<Room, Bid> maxMatching = new LinkedHashMap<Room, Bid>();
   static int numBids = 0;
   final static PrintStream out = new PrintStream(new BufferedOutputStream(System.out));

   public static void main(String args[]) {
      if(args.length <= 0) {
         System.out.println("Need an auction file.");
         System.exit(-1);
      }
      File auctionFile = new File(args[0]);

      try {
         // open the auction file
         Scanner fileReader = new Scanner(auctionFile);

         // get number of rooms and construct the room array and price vector array
         int numRooms = Integer.parseInt(fileReader.nextLine());
         roomsArray = new Room[numRooms];
         priceVector = new int[numRooms];

         // construct each room and their dummy bid, get starting price and put into vector
         for(int x = 0; x < numRooms; x++) {
            String tokens[] = fileReader.nextLine().split(" ");            
            roomsArray[x] = new Room(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), x);
            bids.add(new SingleBid(-1, roomsArray[x].getReservationPrice(), roomsArray[x]));
            priceVector[x] = Integer.parseInt(tokens[2]);
         }

         // match the dummy bids to their rooms in the max mapping
         for(int x = 0; x < roomsArray.length; x++)
            maxMatching.put(roomsArray[x], bids.get(x));

         // read in remaining lines
         while(fileReader.hasNext()) {
            String tokens[] = fileReader.nextLine().split(" ");

            // display the price vector
            if(tokens[0].equals("3")) {
               for(int x = 0; x < priceVector.length; x++)
                  out.print(priceVector[x] + " ");
               out.println();
            }

            // read in a single bid
            else if(tokens[0].equals("1")) {
               SingleBid newestBid = new SingleBid(numBids, Integer.parseInt(tokens[1]), roomsArray[Integer.parseInt(tokens[2])]);
               bids.add(newestBid);
               parseMaxMatching(tokens);
               numBids++;   
               while(pricingAlgorithm()) {}
            }
            // read in a linear bid            
            else if(tokens[0].equals("2")) {
               LinearBid newestBid = new LinearBid(numBids, Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
               bids.add(newestBid);
               parseMaxMatching(tokens);
               numBids++;
               while(pricingAlgorithm()) {}
            }           
         }     
      }
      catch(Exception e) { System.out.println("Error: " + e.toString() + " " + Arrays.toString(e.getStackTrace())); }
      out.close();
   }

   public static boolean pricingAlgorithm() {
      boolean fixedCondition = false;

      // condition one
      for(Map.Entry<Room, Bid> e: maxMatching.entrySet()) {
         Room r1 = e.getKey();
         Bid b = e.getValue();
         if(b instanceof LinearBid) {
            for(int r = 0; r < roomsArray.length; r++) {
               Room r2 = roomsArray[r];
               if(b.getValue(r1) - priceVector[r1.getIndex()] < b.getValue(r2) - priceVector[r2.getIndex()]) {
                  priceVector[r2.getIndex()] += (b.getValue(r2) - priceVector[r2.getIndex()]) - (b.getValue(r1) - priceVector[r1.getIndex()]);
                  fixedCondition = true;
               }
            }
         }
      }

      // condition two
      for(Bid b: bids) {
         if(maxMatching.values().contains(b))
            continue;
         if(b instanceof SingleBid) {
            if(priceVector[((SingleBid)b).getRoom().getIndex()] < b.getValue(null)) {
               priceVector[((SingleBid)b).getRoom().getIndex()] += b.getValue(null) - priceVector[((SingleBid)b).getRoom().getIndex()];
               fixedCondition = true;
            }
         }
         else if(b instanceof LinearBid) {
            for(int r = 0; r < roomsArray.length; r++) {
               if(priceVector[roomsArray[r].getIndex()] < b.getValue(roomsArray[r])) {
                  priceVector[roomsArray[r].getIndex()] += b.getValue(roomsArray[r]) - priceVector[roomsArray[r].getIndex()];
                  fixedCondition = true;
               }
            }
         }
      }
      return fixedCondition;
   }

   // print out the max matching
   public static void printMaxMatching(PrintStream out) {
      for(Map.Entry<Room, Bid> e: maxMatching.entrySet())
         out.print(e.getValue().getID() + " ");
      out.println();
   }

   // parse the max matching out of the input line
   public static void parseMaxMatching(String[] tokens) {
      for(int i = 3; i < tokens.length; i++) {
         Bid currentWinningBid = maxMatching.get(roomsArray[i - 3]);
         int winningBidID = Integer.parseInt(tokens[i]);
         if(currentWinningBid.getID() == winningBidID) {
            continue;
         }
         else if(winningBidID == -1) {
            maxMatching.put(roomsArray[i], bids.get(i));
         }  
         else {
            maxMatching.put(roomsArray[i - 3], bids.get(roomsArray.length + winningBidID));
         }
      }            
   }
}

class Room {
   private int quality;
   private int reservationPrice;
   private int index;

   public Room(int q, int r, int i) {
      quality = q;
      reservationPrice = r;
      index = i;
   }

   public int getQuality() {
      return quality;
   }

   public int getReservationPrice() {
      return reservationPrice;
   }

   public int getIndex() {
      return index;
   }

   public String toString() {
      return "Index: " + index + " Quality: " + quality + " Res Price: " + reservationPrice;
   }
}

class Bid {
   protected int ID;

   public Bid(int i) {
      ID = i;
   }
  
   public int getID() {
      return ID;
   }
 
   public int getValue(Room r) {
      return r.getReservationPrice();
   }
}

class LinearBid extends Bid {
   private int intercept;
   private int slope;

   public LinearBid(int i, int intercept, int slope) {
      super(i);
      this.intercept = intercept;
      this.slope = slope;
   }

   public int getIntercept() {
      return intercept;
   }
   
   public int getSlope() {
      return slope;
   }

   public int getValue(Room r) {
      return intercept + (slope * r.getQuality());
   }

   public String toString() {
      return "Linear Bid, intercept: " + intercept + " slope: " + slope + " ID: " + ID;
   }
}

class SingleBid extends Bid {
   private int bid;
   private Room room;

   public SingleBid(int i, int b, Room r) {
      super(i);
      bid = b;
      room = r;
   }

   public int getValue(Room r) {
      return bid;
   }

   public int getBid() {
      return bid;
   }
   
   public Room getRoom() {
      return room;
   }

   public String toString() {
      return "Single Bid, amount: " + bid + " Room: " + room + " ID: " + ID;
   }
}

