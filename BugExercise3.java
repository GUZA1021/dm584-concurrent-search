import java.util.ArrayList;
import java.util.List;

/*
 * This file implements a simple producer-consumer pattern.
 * The task management takes place through the ArrayList: the producer adds
 * a task in 'tasks', in the forms of numbers-to-be-factorized, and the
 * consumer thread accesses the list and calculates the factorization.
 * However, the implementation is problematic. Can you see why?
 */

/* Solution:
 * The problem is that TaskQueue is not thread-safe. Producer and Worker threads access the shared tasks list at the same time. 
 * This will lead to race conditions e.g task being added while being read. 
 * FIX:
 * We mark addTask() and getTask() as synchrtonized to esnure mutal exclusion.
 * getTask() now waits using wait() when the list is empty, isntead of busy waiting.
 * addTasK() notifies a waiting thread using notify, when it adda  task.
 * We also removed hasTask() method as it accesses the shared list taks without any synchronization and replace with wait() and notify()
 */
public class BugExercise3 {

    static class TaskQueue {
        private List<Integer> tasks = new ArrayList<>();

        public synchronized void addTask(int n) {
            tasks.add(n);
            System.out.println("[Producer] Enqueued task: factor " + n);
            notify(); //Solution: Wake up a waiting consumer
        }

        public synchronized Integer getTask() {
            while (tasks.isEmpty()){
                try {
                    wait(); //Wait for a task to be available
                } catch (InterruptedException  e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return tasks.remove(0);
        }
    }

    static class Producer implements Runnable {
        private final TaskQueue queue;

        public Producer(TaskQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            queue.addTask(123456789);
        }
    }

    static class Worker implements Runnable {
        private final TaskQueue queue;

        public Worker(TaskQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            System.out.println("[Worker] Waiting for tasks...");
            Integer n = queue.getTask();
            if (n != null) {
                System.out.println("[Worker] Task received! Starting factorization...");
                factorize(n);
            }
        }

        private void factorize(int n) {
            for (int i = 2; i <= Math.sqrt(n); i++) {
                while (n % i == 0) {
                    System.out.println("[Worker] Factor: " + i);
                    n /= i;
                }
            }
            if (n > 1) {
                System.out.println("[Worker] Remaining prime factor: " + n);
            }
            System.out.println("[Worker] Done.");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        TaskQueue queue = new TaskQueue();

        Thread worker = new Thread(new Worker(queue));
        Thread producer = new Thread(new Producer(queue));

        worker.start();
        producer.start();

        worker.join();
        producer.join();

        System.out.println("\n--- End of program ---");
    }
}
