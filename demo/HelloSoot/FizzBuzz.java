public class FizzBuzz {
    public void printFizzBuzz(int k){
        if(k%15==0){
            System.out.println("FizzBuzz");
        }
        else if (k%3==0) {
            System.out.println("Fizz");
        }
        else if (k%5==0) {
            System.out.println("Buzz");
        }
        else {
            System.out.println(k);
        }
    }

    public void fizzBuzz(int n){
        for (int i = 0; i < n; i++) {
            printFizzBuzz(i);
        }
    }
}