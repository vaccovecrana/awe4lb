package io.vacco.a4lb;

import java.util.List;
import java.util.Random;

class Backend {
  int weight;
  int priority;

  public Backend(int weight, int priority) {
    this.weight = weight;
    this.priority = priority;
  }
}

public class LoadBalancer {

  private static final Random random = new Random();

  public static Backend getNextBackend(List<Backend> backends) {
    int totalPriorityWeight = backends.stream().mapToInt(backend -> backend.weight * backend.priority).sum();
    int randomPriorityWeight = random.nextInt(totalPriorityWeight);

    int cumulativePriorityWeight = 0;
    for (Backend backend : backends) {
      cumulativePriorityWeight += backend.weight * backend.priority;
      if (randomPriorityWeight < cumulativePriorityWeight) {
        return backend;
      }
    }

    return null;
  }

  public static void main(String[] args) {
    // Create a list of backend servers
    List<Backend> backends = List.of(
        new Backend(1, 3),
        new Backend(2, 2)
    );

    // Perform weighted random selection multiple times
    int host1Count = 0;
    int host2Count = 0;
    int totalSelections = 10000;

    for (int i = 0; i < totalSelections; i++) {
      Backend selectedBackend = getNextBackend(backends);
      if (selectedBackend.weight == 1 && selectedBackend.priority == 1) {
        host1Count++;
      } else if (selectedBackend.weight == 2 && selectedBackend.priority == 2) {
        host2Count++;
      }
    }

    double host1Probability = (double) host1Count / totalSelections;
    double host2Probability = (double) host2Count / totalSelections;

    System.out.println("Host1 probability: " + host1Probability);
    System.out.println("Host2 probability: " + host2Probability);
  }
}
