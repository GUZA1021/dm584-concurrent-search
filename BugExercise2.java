import java.util.List;

/*
 * This file simulates a concurrent scenario in a kitchen.
 * Recipes are simply lists of tools, with the available tools defined as
 * static fields below. To complete a recipe, a Chef needs to
 * acquire (i.e. lock) all tools at the same time. The main() method has an
 * example of two Chefs successfully completing two simple recipes. However, for
 * certain recipes, the Chefs might run into trouble. Can you see why?
 */
public class BugExercise2 {
    static Tool pan = new Tool("Pan");
    static Tool knife = new Tool("Knife");
    static Tool pot = new Tool("Pot");
    static Tool oven = new Tool("Oven");
    static Tool board = new Tool("Cutting Board");

    // Don't change this class
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

    // Don't change this class
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
            List<Tool> tools = recipe.getTools();
                runRecipe(tools, 0);
                System.out.printf("%s finished their recipe!\n", name);
        }

        private void runRecipe(List<Tool> tools, int index) {
            if (index == tools.size())
            {
                return;
            }
            System.out.printf("%s is trying to acquire the %s\n", name, tools.get(index).getName());
            synchronized (tools.get(index)) {
                tools.get(index).use(name);
                runRecipe(tools, index + 1);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // You can experiment with your own recipes
        Recipe recipe1 = new Recipe(List.of(knife,pot,pan));
        Recipe recipe2 = new Recipe(List.of(board,knife,pan));

        Thread chef1 = new Chef("Chef Alice", recipe1);
        Thread chef2 = new Chef("Chef Bob", recipe2);

        chef1.start();
        chef2.start();

        chef1.join();
        chef2.join();

        System.out.println("\n--- All chefs done ---");
    }
}
