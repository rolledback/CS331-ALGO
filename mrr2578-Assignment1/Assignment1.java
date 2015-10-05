import java.util.*;
import java.io.*;

public class Assignment1 {

   static Room rooms[];      
   static ArrayList<Bid> bids = new ArrayList<Bid>();
   static ArrayList<Integer> maxCombo = new ArrayList<Integer>();
   static int maxWeight = -1;

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
            rooms[x] = new Room(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
         }

         while(fileReader.hasNext()) {
            String tokens[] = fileReader.nextLine().split(" ");
            if(tokens[0].equals("1"))
               bids.add(new SingleBid(bids.size(), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])));    
            
            else if(tokens[0].equals("2"))
               bids.add(new LinearBid(bids.size(), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])));
            
            else if(tokens[0].equals("3")) { 
               //System.out.println("----------------------");
               System.out.println("Old max combo: " + maxCombo);                 
               ArrayList<Integer> temp = new ArrayList<Integer>();
               int startValue = Math.min(bids.size() - 1, 0);              
               for(int x = 0; x < rooms.length; x++)
                  temp.add(startValue);
               System.out.println("Initial temp: " + temp);
               maxWeight = calcWeight(temp);
               System.out.println("temp after weight calc: " + temp);
               for(Integer i: temp)
                  maxCombo.add(i.intValue());
               System.out.println("New max: " + maxCombo);
               if(startValue != -1 && rooms.length > 0)
                  nextCombo(temp, 0);

               System.out.print(maxWeight);
               for(Integer i: maxCombo)
                  System.out.print(" " + i);
               System.out.println();
            }
         }        
      }
      catch(Exception e) { System.out.println("Error: " + e.toString()); }      
   }

   static public void nextCombo(ArrayList<Integer> order, int index) {
      System.out.println(order);
      ArrayList<Integer> temp = new ArrayList<Integer>(order);
      int indexValue = temp.get(index);
      int nextValue = indexValue;
      for(int x = -1; nextValue + x < bids.size(); x++) {
         temp.set(index, (nextValue + x));
         int weight = calcWeight(temp);
         if(weight > maxWeight) {
            maxWeight = weight;
            maxCombo.clear();
            for(Integer i: temp)
               maxCombo.add(i.intValue());
            System.out.println("New max: " + maxCombo);
         }
         if(index + 1 < temp.size())
            nextCombo(temp, index + 1);
      }
   }

   static public int calcWeight(ArrayList<Integer> ordering) {
      int weight = 0;
      ArrayList<Integer> assignedBids = new ArrayList<Integer>();
      for(int x = 0; x < ordering.size(); x++) {
         int bidID = ordering.get(x);
         if(assignedBids.contains(bidID))
            return -2;
         else if(bidID != -1)
            assignedBids.add(bidID);
         if(bidID == -1)
            weight += rooms[x].getReservationPrice();
         else {
            Bid bid = bids.get(bidID); 
            if(bid.getClass().equals(SingleBid.class)) {
               if(((SingleBid)bid).getRoom() != x)
                  return -2;
            }
            weight += bid.getValue(rooms[x]);
         }
      }
      return weight;
   }
}

class Room {
   private int quality;
   private int reservationPrice;

   public Room(int q, int r) {
      quality = q;
      reservationPrice = r;
   }

   public int getQuality() {
      return quality;
   }

   public int getReservationPrice() {
      return reservationPrice;
   }

   public String toString() {
      return "Quality: " + quality + " Res Price: " + reservationPrice;
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
      return -1;
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
      return "a: " + a + " b: " + b + " ID: " + ID;
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
      return "Bid : " + bid + "Room: " + room + " ID: " + ID;
   }
}
