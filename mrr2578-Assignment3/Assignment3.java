import java.util.*;
import java.io.*;

public class Assignment3 {

   static Room[] roomsArray;      
   static ArrayList<Room> rooms = new ArrayList<Room>();
   static ArrayList<Bid> bids = new ArrayList<Bid>();
   static ArrayList<LinearBid> linearBids = new ArrayList<LinearBid>();
   static ArrayList<SingleBid> singleBids = new ArrayList<SingleBid>();   
   static LinkedHashMap<Room, Bid> maxMapping = new LinkedHashMap<Room, Bid>();
   static int numBids = 0;
   static int maxWeight = 0;
   final static PrintStream out = new PrintStream(new BufferedOutputStream(System.out));

   public static void main(String args[]) {
      if(args.length <= 0) {
         System.out.println("Need an auction file.");
         System.exit(-1);
      }
      File auctionFile = new File(args[0]);

      try {
         Scanner fileReader = new Scanner(auctionFile);
         int numRooms = Integer.parseInt(fileReader.nextLine());
         roomsArray = new Room[numRooms];
         for(int x = 0; x < numRooms; x++) {
            String tokens[] = fileReader.nextLine().split(" ");            
            rooms.add(new Room(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), x));
            roomsArray[x] = rooms.get(x);
            bids.add(new SingleBid(-1, rooms.get(x).getReservationPrice(), rooms.get(x)));
            singleBids.add((SingleBid)bids.get(x));
         }

         for(int x = 0; x < rooms.size(); x++)
            maxMapping.put(rooms.get(x), bids.get(x));
         maxWeight = calcWeight(maxMapping);

         sortRooms();
         sortSingleBids();  

         while(fileReader.hasNext()) {
            String tokens[] = fileReader.nextLine().split(" ");
            if(tokens[0].equals("1")) {
               SingleBid newestBid = new SingleBid(numBids, Integer.parseInt(tokens[1]), roomsArray[Integer.parseInt(tokens[2])]);
               bids.add(newestBid);
               singleBids.add((SingleBid)bids.get(bids.size() - 1));
               sortSingleBids();
               numBids++;                

               // determine if you are trivally inserting
               Room targetRoom = roomsArray[Integer.parseInt(tokens[2])];
               if(maxMapping.get(targetRoom).getClass().equals(SingleBid.class))
                  trivialInsertion(); 
               else {
                  int bestLinearToRemove = linearBidAlgorithm(singleBids, linearBids);
                  int bestLinearWeight = -1;
                  LinearBid tempLinear = null;
                  if(bestLinearToRemove != -1) {
                     tempLinear = linearBids.remove(bestLinearToRemove);                     
                     bestLinearWeight = calcWeight(constructMapping(singleBids, linearBids));
                     linearBids.add(tempLinear);
                     sortLinearBids();
                  }

                  int bestSingleToRemove = singleBidAlgorithm(singleBids, linearBids);
                  int bestSingleWeight = -1;
                  SingleBid tempSingle = null;
                  if(bestSingleToRemove != -1) {
                     tempSingle = singleBids.remove(bestSingleToRemove);
                     bestSingleWeight = calcWeight(constructMapping(singleBids, linearBids));
                     singleBids.add(tempSingle);
                     sortSingleBids();
                  }

                  maxWeight = Math.max(maxWeight, Math.max(bestLinearWeight, bestSingleWeight));
                  if(maxWeight == bestLinearWeight && bestLinearToRemove != -1) {
                     linearBids.remove(tempLinear);
                     maxMapping = constructMapping(singleBids, linearBids);
                  }
                  else if(maxWeight == bestSingleWeight && bestSingleToRemove != -1) {
                     singleBids.remove(tempSingle);
                     maxMapping = constructMapping(singleBids, linearBids);
                  }  
                  else {
                     singleBids.remove(newestBid);
                     bids.remove(newestBid);
                  }          
               }
            }            
            else if(tokens[0].equals("2")) {
               LinearBid newestBid = new LinearBid(numBids, Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
               bids.add(newestBid);
               linearBids.add((LinearBid)bids.get(bids.size() - 1));
               sortLinearBids();
               numBids++;

               int bestLinearToRemove = linearBidAlgorithm(singleBids, linearBids);
               int bestLinearWeight = -1;
               LinearBid tempLinear = null;
               if(bestLinearToRemove != -1) {
                  tempLinear = linearBids.remove(bestLinearToRemove);                     
                  bestLinearWeight = calcWeight(constructMapping(singleBids, linearBids));
                  linearBids.add(tempLinear);
                  sortLinearBids();
               }
               int bestSingleToRemove = singleBidAlgorithm(singleBids, linearBids);
               int bestSingleWeight = -1;
               SingleBid tempSingle = null;
               if(bestSingleToRemove != -1) {
                  tempSingle = singleBids.remove(bestSingleToRemove);
                  bestSingleWeight = calcWeight(constructMapping(singleBids, linearBids));
                  singleBids.add(tempSingle);
                  sortSingleBids();
               }

               maxWeight = Math.max(maxWeight, Math.max(bestLinearWeight, bestSingleWeight));
               if(maxWeight == bestLinearWeight && bestLinearToRemove != -1) {
                  linearBids.remove(tempLinear);
                  maxMapping = constructMapping(singleBids, linearBids);
               }
               else if(maxWeight == bestSingleWeight && bestSingleToRemove != -1) {
                  singleBids.remove(tempSingle);
                  maxMapping = constructMapping(singleBids, linearBids);
               }
               else {
                  linearBids.remove(newestBid);
                  bids.remove(newestBid);
               }
                  
            }           
            else if(tokens[0].equals("3")) {
               int weightCheck = 0;
               int[] maxCombo = new int[roomsArray.length];
               for(Map.Entry<Room, Bid> e: maxMapping.entrySet()) {
                  Room r = e.getKey();
                  Bid b = e.getValue();
                  maxCombo[r.getIndex()] = b.getID();
                  weightCheck += b.getValue(r);
               }
               out.print(calcWeight(maxMapping));
               for(int x = 0; x < maxCombo.length; x++)
                  out.print(" " + maxCombo[x]);
               out.println();
            }
         }     
      }
      catch(Exception e) { System.out.println("Error: " + e.toString()); }
      out.close();
   }
   
   public static void sortRooms() {
      Collections.sort(rooms, new Comparator<Room>() {
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

   public static void sortSingleBids() {
      Collections.sort(singleBids, new Comparator<SingleBid>() {
         public int compare(SingleBid B1, SingleBid B2) {
            return rooms.indexOf(B1.getRoom()) - rooms.indexOf(B2.getRoom());
         }
      });
   }

   public static void sortLinearBids() {
      Collections.sort(linearBids, new Comparator<LinearBid>() {
         @Override
         public int compare(LinearBid L1, LinearBid L2) {
            if(L1.getB() > L2.getB())
               return 1;
            if(L1.getB() < L2.getB())
               return -1;
            return 0;
         }
      });      
   }

   public static int checkSum(int[] assignments) {
      int weight = 0;
      for(int x = 0; x < assignments.length; x++)
         if(assignments[x] != -1)
            for(int b = 0; b < bids.size(); b++) {
               if(bids.get(b).getID() == assignments[x]) {
                  weight += bids.get(b).getValue(rooms.get(x));
                  break;
               }
            }
         else
            weight += rooms.get(x).getReservationPrice();
      return weight;
   }

   public static void trivialInsertion() {
      // get newest bid, guaranteed to be a single bid
      SingleBid newBid = ((SingleBid)bids.get(bids.size() - 1));
      int newValue = newBid.getValue(null);
      Room targetRoom = roomsArray[newBid.getRoom().getIndex()];
      // get the bid currentlly mapped to the target room
      Bid currentBid = maxMapping.get(targetRoom);
      int currentValue = currentBid.getValue(targetRoom);

      // if the current bid is single, then trivial insertion
      if(currentBid.getClass().equals(SingleBid.class)) {
         // if the new bid has a greater value then current value
         // remove the current bid from both bids list and single bids list
         // map the new bid to the targetID
         // else remove newBid from everywhere
         if(newValue > currentValue) {
            bids.remove(currentBid);
            singleBids.remove((SingleBid)currentBid);
            maxMapping.put(targetRoom, newBid);
         }
         else {
            bids.remove(newBid);
            singleBids.remove(newBid);
         }
      }
   }

   public static int singleBidAlgorithm(ArrayList<SingleBid> singleSet, ArrayList<LinearBid> linearSet) {
      int matchingValue = 0;
      int maxMatchingValue = maxWeight;
      int bidToRemove = -1;

      int[] whereLinearBidsAre = new int[linearSet.size()];
      
      // produce a list of unmatched rooms by matching all 
      // but first single bid to targets, O(n) + O(|A| - 1) = O(n)
      ArrayList<Room> unMatchedRooms = new ArrayList<Room>();
      for(Room r: rooms)
         unMatchedRooms.add(r);      

      if(singleBids.size() == 0)
         return -1;
      for(int sB = 1; sB < singleSet.size(); sB++) {
         matchingValue += singleSet.get(sB).getValue(null);
         unMatchedRooms.remove(singleSet.get(sB).getRoom());
      }

      // add to the initial value all the linear bids now matched
      // also keep track of what rooms they are matched to, O(|B|)
      for(int lB = 0; lB < linearSet.size(); lB++) {
         matchingValue += linearSet.get(lB).getValue(unMatchedRooms.get(lB));
         whereLinearBidsAre[lB] = rooms.indexOf(unMatchedRooms.get(lB));
      }
      if(matchingValue > maxMatchingValue) {
         maxMatchingValue = matchingValue;
         bidToRemove = 0;
      }

      // try removing all other single bids
      for(int i = 1; i < singleSet.size(); i++) {
         matchingValue += singleSet.get(i - 1).getValue(null);
         int lowerBound = rooms.indexOf(singleSet.get(i - 1).getRoom());
         matchingValue -= singleSet.get(i).getValue(null);
         int higherBound = rooms.indexOf(singleSet.get(i).getRoom());
         for(int lBP = 0; lBP < whereLinearBidsAre.length; lBP++) {
            if(whereLinearBidsAre[lBP] >= lowerBound && whereLinearBidsAre[lBP] <= higherBound) {
               matchingValue -= linearSet.get(lBP).getValue(rooms.get(whereLinearBidsAre[lBP]));
               whereLinearBidsAre[lBP]++;
               matchingValue += linearSet.get(lBP).getValue(rooms.get(whereLinearBidsAre[lBP]));
            }
         }
         if(matchingValue > maxMatchingValue) {
            maxMatchingValue = matchingValue;
            bidToRemove = i;
         }         
      }
      return bidToRemove;
         
   }
   
   // O(n) + O(|B|) + O(|A| - 1) + O(|A| - 1) = O(n)
   public static int linearBidAlgorithm(ArrayList<SingleBid> singleSet, ArrayList<LinearBid> linearSet) {
      int matchingValue = 0;
      int maxMatchingValue = maxWeight;
      int bidToRemove = -1;
      
      // produce a list of unmatchedRooms, O(n) + O(|B|) = O(n)
      ArrayList<Room> unMatchedRooms = new ArrayList<Room>();
      for(Room r: rooms)
         unMatchedRooms.add(r);
      for(SingleBid b: singleSet) {
         unMatchedRooms.remove(b.getRoom());
         matchingValue += b.getValue(null);
      }
      
      // create the initial M(0) which excludes the first linear bid and set the
      // maxMatching to initially be this matching's weight, and the bidToRemove
      // as the 0th bid, O(|A| - 1)
      for(int i = 1; i < linearSet.size(); i++)
         matchingValue += linearSet.get(i).getValue(unMatchedRooms.get(i - 1));
      if(matchingValue > maxMatchingValue)
         maxMatchingValue = matchingValue;
      bidToRemove = 0;

      // calc the rest of the M's, excluding bid i+1 each time, O(|A| - 1) 
      for(int i = 0; i < linearSet.size() - 1; i++) {
         matchingValue = matchingValue - linearSet.get(i + 1).getValue(unMatchedRooms.get(i)) + linearSet.get(i).getValue(unMatchedRooms.get(i));
         if(matchingValue > maxMatchingValue) {
            maxMatchingValue = matchingValue;
            bidToRemove = i + 1;
         }
      }
      return bidToRemove;
   }

   public static LinkedHashMap<Room, Bid> constructMapping(ArrayList<SingleBid> singleSet, ArrayList<LinearBid> linearSet) {
      int checksum = 0;
      LinkedHashMap<Room, Bid> mapping = new LinkedHashMap<Room, Bid>();
      ArrayList<Room> unMatchedRooms = new ArrayList<Room>();
      for(Room r: rooms)
         unMatchedRooms.add(r);

      for(SingleBid b: singleSet) {
         unMatchedRooms.remove(b.getRoom());
         mapping.put(b.getRoom(), b);
         checksum += b.getValue(null);
      }

      for(int b = 0; b < linearSet.size(); b++) {
         mapping.put(unMatchedRooms.get(b), linearSet.get(b));
         checksum += linearSet.get(b).getValue(unMatchedRooms.get(b));
      }
      return mapping;
   }

   public static int calcWeight(LinkedHashMap<Room, Bid> mapping) {
      int weight = 0;  
      for(Map.Entry<Room, Bid> e: mapping.entrySet()) {
         Room r = e.getKey();
         Bid b = e.getValue();
         weight += b.getValue(r);
      }
      return weight;
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
   private int a;
   private int b;

   public LinearBid(int i, int a, int b) {
      super(i);
      this.a = a;
      this.b = b;
   }

   public int getA() {
      return a;
   }
   
   public int getB() {
      return b;
   }

   public int getValue(Room r) {
      return a + (b * r.getQuality());
   }

   public String toString() {
      return "Linear Bid, a: " + a + " b: " + b + " ID: " + ID;
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

