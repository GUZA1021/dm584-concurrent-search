import java.util.*;

/*
 * This file simulates a discount coupon mechanism. Each coupon, originating
 * from a fixed, starting pool, can only be used once. Upon being used, the
 * coupon's value (set to 10) is added to the total discount.
 */


 /* Solution:
  * Problem is a race condition. One thread checks couponPool.contains(couponId) and sees if its present.
  * Before it can remove the coupon, another thread also check and sees it too, this will result in both threads adding the discount.
  * Then only one of them removes the coupon, leading to over counting of the discount. 
  * A way to fix it is to make it so checking, add discount and coupon removal atomic. We can synchronize them under a signle lock, then we prevent
  * multiple threads from seeing the same valid coupon before its removed.
  */
public class BugExercise1 {

    public static final int STARTINGCOUNT = 5000;
    public static final int VALUE = 10;
    public static final List<Integer> ISSUED_COUPON_IDS = Collections.unmodifiableList(
        new ArrayList<>() {{
            for (int i = 0; i < STARTINGCOUNT; i++) add(i);
        }}
    );

    static class Store {
        private final Set<Integer> couponPool = new HashSet<>(ISSUED_COUPON_IDS);
        public final int couponValue = VALUE;
        private int totalDiscountGiven = 0;

        private final Object couponLock = new Object();
        private final Object discountLock = new Object();

        public void checkout(String user, Integer couponId) {
            synchronized (couponLock) {
                if (!couponPool.contains(couponId)) {
                    return;
                }
                 couponPool.remove(couponId);
                totalDiscountGiven += couponValue;
            }
        }

        public int getTotalDiscountGiven() {
            synchronized (discountLock) {
                return totalDiscountGiven;
            }
        }

        public int getRemainingCoupons() {
            synchronized (couponLock) {
                return couponPool.size();
            }
        }

    
    }

    /*
     * Don't change this class.
     * Well, technically you can, but there is no need to.
     * This is "client" code that demonstrates the issue.
     */
    static class User extends Thread {
        private final Store store;
        private final String userId;

        public User(Store store, String userId) {
            this.store = store;
            this.userId = userId;
        }

        @Override
        public void run() {
            for (Integer couponId : ISSUED_COUPON_IDS) {
                store.checkout(userId, couponId);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Store store = new Store();
        int startingCoupons = store.getRemainingCoupons();

        Thread user1 = new User(store, "Alice");
        Thread user2 = new User(store, "Bob");

        user1.start();
        user2.start();

        user1.join();
        user2.join();

        int expectedMax = startingCoupons * store.couponValue;
        int actual = store.getTotalDiscountGiven();

        System.out.println("\n--- Final Report ---");
        System.out.println("Total discount given: " + actual + " DKK");
        System.out.println("Expected discount: " + expectedMax + " DKK");

        if (actual > expectedMax) {
            System.out.println("FRAUD DETECTED: Coupons reused!");
        } else if (actual < expectedMax) {
            System.out.println("EVASION DETECTED: Less discount given!");
        } else {
            System.out.println("All good: Coupons respected.");
        }
    }
}
