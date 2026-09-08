import java.util.*;

/*
 * This file simulates a concurrent scenario in a kitchen.
 * Recipes are simply lists of tools, with the available tools defined as
 * static fields below. To complete a recipe, a Chef needs to
 * acquire (i.e. lock) all tools at the same time. The main() method has an
 * example of two Chefs successfully completing two simple recipes. However, for
 * certain recipes, the Chefs might run into trouble. Can you see why?
 */

/* Solution:
 * The problem is deadlock. Each Chef locks the tools one at a time, nested, in
 * the order they appear in its own recipe. If two Chefs need overlapping tools
 * in opposite relative order, each ends up holding a lock the other is waiting
 * for, and neither can ever proceed.
 *
 * All four Coffman conditions are present: the tools are held under mutual
 * exclusion, a Chef holds locks while waiting for the next one, locks are not
 * preemptible, and the recipes can form a circular wait.
 *
 * The recipes in main() happen not to deadlock, because both Chefs acquire the
 * knife before the pan, so no cycle forms. Swapping to
 *     recipe1 = List.of(knife, pan)
 *     recipe2 = List.of(pan, knife)
 * makes the program hang immediately: Alice holds the knife and waits for the
 * pan, Bob holds the pan and waits for the knife.
 *
 * FIX:
 * We break the circular wait by imposing a global lock ordering. Before
 * acquiring anything, a Chef sorts its tools by name and locks them in that
 * order. Since every Chef follows the same order, no cycle can form, so
 * deadlock is impossible.
 *
 * Once all locks are held, the tools are used in the recipe's own order, so
 * the visible behaviour of the recipe is unchanged. The recursion is kept so
 * that all locks are held simultaneously, as the task requires.
 */
public class BugExercise2 {

    static Tool pan = new Tool("Pan");
    static Tool knife = new Tool("Knife");
    static Tool pot = new Tool("Pot");
    static Tool oven = new Tool("Oven");
    static Tool board = new Tool("Cutting Board");

    // Don't change this class  [provided by the course]
    static class Tool {
        private final String name;

        public Tool(String name) {
            this.name = name;
        }

        public void use(String chefName) {
            System.out.printf("%s is using the %s\n", chefName, name);
            simulateWork();
        }

        public String getName() {
            return name;
        }

        private void simulateWork() {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }
    }

    // Don't change this class  [provided by the course]
    static class Recipe {
        private final List<Tool> tools;

        public Recipe(List<Tool> tools) {
            this.tools = tools;
        }

        public List<Tool> getTools() {
            return tools;
        }
    }

    static class Chef extends Thread {
        private final String name;
        private final Recipe recipe;

        public Chef(String name, Recipe recipe) {
            this.name = name;
            this.recipe = recipe;
        }

        @Override
        public void run() {
            List<Tool> useOrder = recipe.getTools();

            // Solution: acquire in a globally consistent order, not recipe order
            List<Tool> lockOrder = new ArrayList<>(useOrder);
            lockOrder.sort(Comparator.comparing(Tool::getName));

            acquireAll(lockOrder, 0, useOrder);
            System.out.printf("%s finished their recipe!\n", name);
        }

        /*
         * Acquires every lock in lockOrder, holding them all at once, and only
         * then uses the tools in the recipe's original order.
         */
        private void acquireAll(List<Tool> lockOrder, int index, List<Tool> useOrder) {
            if (index == lockOrder.size()) {
                for (Tool tool : useOrder) {
                    tool.use(name);
                }
                return;
            }

            System.out.printf("%s is trying to acquire the %s\n",
                              name, lockOrder.get(index).getName());

            synchronized (lockOrder.get(index)) {
                acquireAll(lockOrder, index + 1, useOrder);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // You can experiment with your own recipes.
        // These two deadlock in the original implementation:
        //   Recipe recipe1 = new Recipe(List.of(knife, pan));
        //   Recipe recipe2 = new Recipe(List.of(pan, knife));
        Recipe recipe1 = new Recipe(List.of(knife, pot, pan));
        Recipe recipe2 = new Recipe(List.of(board, knife, pan));

        Thread chef1 = new Chef("Chef Alice", recipe1);
        Thread chef2 = new Chef("Chef Bob", recipe2);

        chef1.start();
        chef2.start();

        chef1.join();
        chef2.join();

        System.out.println("\n--- All chefs done ---");
    }
}
