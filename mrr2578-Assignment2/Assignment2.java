import java.util.*;
import java.io.*;

public class Assignment2 {

   static Room rooms[];      
   static ArrayList<Bid> bids = new ArrayList<Bid>();
   static LinkedHashMap<Room, Bid> maxMapping = new LinkedHashMap<Room, Bid>();
   static int numBids = 0;
   static int maxWeight = 0;

   public static void main(String args[]) {
      if(args.length <= 0) {
         System.out.println("Need an auction file.");
         System.exit(-1);
      }
      File auctionFile = new File(args[0]);

      try {
         Scanner fileReader = new Scanner(auctionFile);
         rooms = new Room[Integer.parseInt(fileReader.nextLine())];
         for(int x = 0; x < rooms.length; x++) {
            String tokens[] = fileReader.nextLine().split(" ");            
            rooms[x] = new Room(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), x);
            bids.add(new DummyBid(-1, x, rooms[x]));
         }

         for(int x = 0; x < rooms.length; x++)
            maxMapping.put(rooms[x], bids.get(x));
         maxWeight = calcWeight(maxMapping);

         while(fileReader.hasNext()) {
            String tokens[] = fileReader.nextLine().split(" ");
            if(tokens[0].equals("1")) {
               bids.add(new SingleBid(numBids, Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])));
               numBids++;  
               newSingleBid();
            }            
            else if(tokens[0].equals("2")) {
               bids.add(new LinearBid(numBids, Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])));
               numBids++;
               newLinearBid();
            }           
            else if(tokens[0].equals("3")) {
               int[] maxCombo = new int[rooms.length];
               for(Map.Entry<Room, Bid> e: maxMapping.entrySet()) {
                  Room r = e.getKey();
                  Bid b = e.getValue();
                  maxCombo[r.getIndex()] = b.getID();
               }
               System.out.print(calcWeight(maxMapping));
               for(int x = 0; x < maxCombo.length; x++)
                  System.out.print(" " + maxCombo[x]);
               System.out.println();
            }
         }        
      }
      catch(Exception e) { System.out.println("Error: " + e.toString()); }
   }
   
   public static int checkSum(int[] assignments) {
      int weight = 0;
      for(int x = 0; x < assignments.length; x++)
         if(assignments[x] != -1)
            for(int b = 0; b < bids.size(); b++) {
               if(bids.get(b).getID() == assignments[x]) {
                  weight += bids.get(b).getValue(rooms[x]);
                  break;
               }
            }
         else
            weight += rooms[x].getReservationPrice();
      return weight;
   }

   public static void newSingleBid() {
      // get newest bid, guaranteed to be a single bid
      SingleBid newBid = ((SingleBid)bids.get(bids.size() - 1));
      int targetID = newBid.getRoom();
      int competingBid = newBid.getValue(null);
      int currentBid = maxMapping.get(rooms[targetID]).getValue(rooms[targetID]);
      // compare new bid's bid to its target's current winning bid's bid
      LinearBid savedBid = null;
      if(competingBid > currentBid || maxMapping.get(rooms[targetID]).getClass().equals(LinearBid.class)) {
         // if it wins, remove the old bid from bid list and remap the room to the new bid
         if(maxMapping.get(rooms[targetID]).getClass().equals(LinearBid.class))
            savedBid = ((LinearBid)maxMapping.get(rooms[targetID]));
         bids.remove(maxMapping.get(rooms[targetID]));
         maxMapping.put(rooms[targetID], newBid);
      }  
      else // else remove the new big cause he's a loser
         bids.remove(newBid);
      maxWeight = calcWeight(maxMapping);
      // if you removed a linear bid, see if there's a spot for it 
      if(savedBid != null) {
         bids.add(savedBid);
         newLinearBid();
      }
   }
   
   public static void newLinearBid() {
      int bidToRemove = bids.size() - 1;
      for(int b = bidToRemove; b > -1; b--) {
         ArrayList<Bid> temp = new ArrayList<Bid>();         
         for(Bid x: bids)
            temp.add(x);
         temp.remove(b);
         LinkedHashMap<Room, Bid> tempMapping = subsetMapping(temp);
         int tempWeight = calcWeight(tempMapping);
         if(tempWeight > maxWeight) {
            bidToRemove = b;
            maxMapping = tempMapping;
            maxWeight = tempWeight;
         }
      }
      bids.remove(bidToRemove);    
   }

   public static LinkedHashMap<Room, Bid> subsetMapping(ArrayList<Bid> subset) {
      ArrayList<LinearBid> linearBids = new ArrayList<LinearBid>();
      LinkedHashMap<Room, Bid> mapping = new LinkedHashMap<Room, Bid>();
      // go through subset of remaining bids, if single or dummy bid, map to correct room, if linear add to linear list      
      for(Bid b: subset) {
         if(b.getClass().equals(SingleBid.class))
            mapping.put(rooms[((SingleBid)b).getRoom()], b);
         else if(b.getClass().equals(DummyBid.class))
            mapping.put(rooms[((DummyBid)b).getRoom()], b);
         else
            linearBids.add((LinearBid)b);
      }
      // add all unassigned rooms to the unassigned rooms list
      ArrayList<Room> unassignedRooms = new ArrayList<Room>();
      for(int r = 0; r < rooms.length; r++)
         if(!mapping.containsKey(rooms[r]))
            unassignedRooms.add(rooms[r]);
      // sort each list
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
      Collections.sort(unassignedRooms, new Comparator<Room>() {
         @Override
         public int compare(Room R1, Room R2) {
            if(R1.getQuality() > R2.getQuality())
               return 1;
            if(R1.getQuality() < R2.getQuality())
               return -1;
            return 0;
         }
      });
      // make da mappings
      for(int m = 0; m < linearBids.size(); m++)
         mapping.put(unassignedRooms.get(m), linearBids.get(m));
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

class DummyBid extends Bid {
   protected int room;
   protected Room dummy;

   public DummyBid(int i, int r, Room d) {
      super(i);
      room = r;
      dummy = d;
   }

   public int getRoom() {
      return room;
   }

   public int getValue(Room r) {
      return r.getReservationPrice();
   }

   public String toString() {
      return "Dummy bid for room: " + room + " with res value " + dummy.getReservationPrice();
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
   private int room;

   public SingleBid(int i, int b, int r) {
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
   
   public int getRoom() {
      return room;
   }

   public String toString() {
      return "Single Bid, amount: " + bid + " Room: " + room + " ID: " + ID;
   }
}

