import java.util.*;
import java.io.*;

public class Assignment6 {

   static Room[] beta;      
   static Room[] rooms;
   static int[] priceVector;
   static HashMap<Integer, Bid> quickBidAccess = new HashMap<Integer, Bid>();
   static LinkedHashMap<Room, Bid> maxMatching = new LinkedHashMap<Room, Bid>();
   static int numBids = 0;
   static Bid unmatchedBid = null;
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

         // get number of beta and construct the room array and price vector array
         int numRooms = Integer.parseInt(fileReader.nextLine());
         beta = new Room[numRooms];
         rooms = new Room[numRooms];
         priceVector = new int[numRooms];

         // construct each room and stuffs
         for(int x = 0; x < numRooms; x++) {
            String tokens[] = fileReader.nextLine().split(" ");            
            beta[x] = new Room(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), x);
            rooms[x] = beta[x];
            priceVector[x] = Integer.parseInt(tokens[2]);
            SingleBid dummyBid = new SingleBid(-1, beta[x].getReservationPrice(), beta[x]);
            quickBidAccess.put(new Integer((x * -1) - 1), dummyBid);
            maxMatching.put(beta[x], dummyBid);
         }
         sortBeta();
         System.out.println("Starting price vector: " + Arrays.toString(priceVector));

         // read in remaining lines
         while(fileReader.hasNext()) {
            String tokens[] = fileReader.nextLine().split(" ");
            System.out.println("New line: " + Arrays.toString(tokens));
            // display the price vector
            if(tokens[0].equals("3")) {
               for(int x = 0; x < priceVector.length; x++)
                  out.print(priceVector[x] + " ");
               out.println();
            }
            else {
               System.out.println("Reading in a bid.");
               // read in a single bid
               Bid newestBid = null;
               if(tokens[0].equals("1"))
                  newestBid = new SingleBid(numBids, Integer.parseInt(tokens[1]), rooms[Integer.parseInt(tokens[2])]);

               // read in a linear bid            
               else if(tokens[0].equals("2"))
                  newestBid = new LinearBid(numBids, Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
               
               quickBidAccess.put(new Integer(numBids), newestBid);
               parseMaxMatching(tokens);
               numBids++;
               pricingAlgorithm();
               System.out.println("New price vector: " + Arrays.toString(priceVector));
            }         
         }     
      }
      catch(Exception e) { System.out.println("Error: " + e.toString() + " " + Arrays.toString(e.getStackTrace())); }
      out.close();
   }

   public static void sortBeta() {
      Arrays.sort(beta, new Comparator<Room>() {
         public int compare(Room R1, Room R2) {
            if(R1.getQuality() > R2.getQuality())
               return 1;
            if(R1.getQuality() < R2.getQuality())
               return -1;
            else
               return R1.getIndex() - R2.getIndex();
         }
      });   
   }

   public static Bid alpha(int i) {
      return maxMatching.get(beta[i]);
   }

   public static int sigma(int i) {
      System.out.println("sigma(" + i + ")");
      for(int x = i - 1; x > -1; x--) {
         System.out.println("x = " + x + ", alpha(x) = " + alpha(x));
         if(alpha(x) instanceof LinearBid)
            return x;
      }
      return -1;
   }

   public static int tau(int i) {
      System.out.println("tau(" + i + ")");
      for(int x = i + 1; x < beta.length; x++) {
         System.out.println("x = " + x + ", alpha(x) = " + alpha(x));
         if(alpha(x) instanceof LinearBid)
            return x;
      }
      return beta.length;
   }

   public static int f(int i, int j) {
      System.out.println("f(" + i + ", " + j + ") = " + (priceVector[beta[j].getIndex()] - priceVector[beta[i].getIndex()]));
      return priceVector[beta[j].getIndex()] - priceVector[beta[i].getIndex()];
   }

   public static int g(int i, int j) {
      System.out.println("f(" + i + ", " + j + ") = " + (beta[j].getQuality() - beta[i].getQuality()));
      return beta[j].getQuality() - beta[i].getQuality();
   }

   public static int s(int i) {
      System.out.println("s(" + i + ") = " + (((LinearBid)alpha(i)).getSlope()));
      return ((LinearBid)alpha(i)).getSlope();
   }

   public static int gap(int i, int j) {
      System.out.println("gap(" + i + ", " + j + ")");
      if(i == -1 || i == beta.length) {
         System.out.println("i = -1 or beta.lengh");
         return 0;
      }
      if(alpha(i) instanceof SingleBid) {
         System.out.println("single bid instance");
         return 0;
      }
      return Math.max(0, (s(i) * g(i, j)) - f(i, j));
   }

   public static void update(int i, int j) {
      priceVector[beta[j].getIndex()] = priceVector[beta[j].getIndex()] + gap(i, j);
   }

   public static void pricingAlgorithm() {
      System.out.println("Begin pricing algorithm.");
      if(unmatchedBid != null) {
         System.out.println("Unmatched bid: " + unmatchedBid);
         if(unmatchedBid instanceof SingleBid) {
            if(priceVector[((SingleBid)unmatchedBid).getRoom().getIndex()] < unmatchedBid.getValue(null)) {
               System.out.println("Unmatched bid's offer on its target less than price vector, update!");
               priceVector[((SingleBid)unmatchedBid).getRoom().getIndex()] = unmatchedBid.getValue(null);
            }
         }
         else {
            for(int r = 0; r < beta.length; r++)
               if(priceVector[beta[r].getIndex()] < unmatchedBid.getValue(beta[r])) {
                  System.out.println("Unmatched bid's offer on its target less than price vector, update!");
                  priceVector[beta[r].getIndex()] = unmatchedBid.getValue(beta[r]);
               }
         }
         System.out.println("Removing the unmatched bid.");
         if(unmatchedBid.getID() != -1)
            quickBidAccess.remove(unmatchedBid.getID());
         else
            quickBidAccess.remove((((SingleBid)unmatchedBid).getRoom().getIndex() * -1) - 1);
         unmatchedBid = null;
      }
      System.out.println("First for loop, left to right.");
      for(int i = 0; i < beta.length; i++) {
         int g = gap(sigma(i), i);
         System.out.println("Gap returned: " + g);
         if(g > 0)
            update(sigma(i), i);
      }
      System.out.println("Second for loop, right to left.");
      for(int i = beta.length - 1; i > -1; i--) {
         int g = gap(tau(i), i);
         System.out.println("Gap returned: " + g);
         if(g > 0)
            update(tau(i), i);
      }
   }
   
   public static void printMaxMatching(PrintStream out) {
      for(Map.Entry<Room, Bid> e: maxMatching.entrySet())
         out.print(e.getValue().getID() + " ");
      out.println();
   }

   public static void parseMaxMatching(String[] tokens) {
      System.out.println("Parsing the line.");
      ArrayList<Bid> currentMatchedBids = new ArrayList<Bid>(beta.length);
      for(Bid b: quickBidAccess.values())
         currentMatchedBids.add(b);
      for(int i = 3; i < tokens.length; i++) {
         Bid currentWinningBid = maxMatching.get(rooms[i - 3]);
         int winningBidID = Integer.parseInt(tokens[i]);
         if(winningBidID != -1) {
            Bid b = quickBidAccess.get(winningBidID);
            maxMatching.put(rooms[i - 3], b);
            currentMatchedBids.remove(b);
         }
         else {
            Bid b = quickBidAccess.get((i - 3) * (-1) - 1);
            maxMatching.put(rooms[i - 3], b);
            currentMatchedBids.remove(b);
         }
      }
      unmatchedBid = currentMatchedBids.get(0);
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
      return "Linear Bid, ID: " + ID;
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
      return "Single Bid, ID: " + ID;
   }
}

