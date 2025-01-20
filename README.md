# CLD_Analysis
Code for analysis of Causal Loop Diagrams

Causal Loop Diagrams are diagrams that establish categories representing real-world entities or concepts and encode positive and negative influences among them. An important component of these is the existence of loops that can create cycles. For example, suppose that "Tax Revenue" affects "Quality of Schools" in a positive way (more of the former leads to more of the latter), which in turn affects "Labor Force Education Level", also in a positive way; "Labor Force Education Level" has a positive effect on "Tax Revenue". In this situation, an increase in any one of these will lead to increases in the others, potentially reinforcing the effects as they cycle through the loop.

Cycles may be reinforcing, in which case they will lead to increases (or decreases) that persist and may even accelerate as the effects of any initial change traverse the loop and result in further changes in the same direction; they may also be damping, in which case an initial change in one direction causes a sequence of other changes that affect the initial entity in the opposite way (e.g. a positive change leads to a negative influence).

Most examples will be more complicated in pratice, involving large numbers of entities and multiple incoming effects and outgoing influences from each one. The use of the diagram is to try to establish strategies to influence the real-world system in positive ways by influencing specific entities.

The first step in performing analysis of these is to identify loops. Nodes that have no inputs are 'sources' and cannot participate in loops; nodes that have no influences are 'sinks' and also cannot participate in loops. Once these are removed (recursively, until no examples remain), the remaining nodes must participate in at least one loop. This may be a 'grand loop' that includes every node; or it may include multiple disconnected network components, with nodes in one component not connected in any way to nodes in another. Any collection of connected nodes could include multiple loops, such that any entity may participate in multiple loops simultaneously.

A key task in understanding the Causal Loop Diagram is identifying nodes that play an outsize role in the overall network. Consider a simple case of two reinforcing loops, one with nodes "A - B - C" and one with nodes "C - D - E". (Note: The notation omits the last step, which is to connect back to the first entity and complete the loop.) Here we would say that 'C' could be a key point of leverage over the system, as changes to C will impact nodes A, B, D, and E. We would say that 'C' participates in two loops.

However, consider a complication in a network that includes connections from A to both B and C, and from both B and C to D. This would be considered two distinct loops: "A - B - D" and "A - C - D". But can we say that 'A' participates in two loops? Intuitively, it appears more like there is one loop with a small variation - whether it goes through B or C on its way to D.

This complication becomes even more troubling if we assume that there are multiple pairs similar to B and C, so the path goes "A - [B or C] - D - [E or F] - G - [H or I] - J". In this case there are 2^3 loops ("A - B - E - G - H -J", "A - C - E - G - H - J", etc.), but all of these loops are fundamentally just a loop from A through J (and back to A). Note that the same thing can happen if two loops differ by skipping a node, e.g. "A - B - C - D" and "A - B - D".

Because the number of loops can grow exponentially, it is challenging to identify which nodes are the most critical in promoting change in the network. This code is intended to offer a scoring system that:

* Inventories all nodes in the diagram and assigns each one an integer identifier
* Inventories all the loops in the diagram
* For each target node, inventories all of the loops in which that node participates
* Represents each loop as a sequence of entity identifiers
* Calculates the Levenshtein distance between all pairs of loops
* Assigns a score to each entity based on the number of distinct groups in which that entity participates, scaling each loop based on its similarity to other loops so that fully distinct loops count more fully than loops that are merely variants of each other

The end result is a ranked list of the nodes based on their scores.

For full documentation, see:

Anton Rozhkov, Moira Zellner, John T. Murphy, Dean Massey: "Identifying leverage points for sustainable transitions in urban – rural systems: Application of graph theory to participatory causal loop diagramming" Environmental Science & Policy, Volume 164, 2025, https://doi.org/10.1016/j.envsci.2025.103996

# Input data

The input data should be a csv file in the format:

```
Source,Target,Polarity
Entity1,Entity2,Positive
Entity1,Entity3,Positive
Entity2,Entity3,Negative
Entity3,Entity4,Positive
Entity4,Entity1,Positive
...
```

Note that entity names cannot be quoted or contain commas. "Positive" must be spelled exactly (case sensitive), and anything that is not a match will be considered negative polarity.

# Usage

Typically usage will involve the LoopSetLoader class. Generally it will follow this pattern:

```
LoopSetLoader loader = new LoopSetLoader();
loader.loadLoopSet(path, inputFilename);
```

The loader will populate the `ConceptFactory` with a unique list of all the entities, create an instance of the `DiagramNetwork` representing the links among the entities, and a `LoopSet` that will have all of the loops found in the network. Note that these classes can also be called outside the LoopSetLoader if they are to be used in isolation. On load, the LoopSetLoader also includes a Scores collection, but this is not yet initialized.

You can write intermediate or debugging files by calling some of the helper functions:

```
loader.writeConceptNodeFile(filename);
loader.writeConceptLinkFile(filename);
loader.writeLoopNodeFile(filename);
loader.writeLoopLinkFile(filename, 1l); // Indicate a threshold or a limit, or you could get a million!!!
```

## Calculating Scores

For small data sets, calculating scores can be done by calling:

```
loader.getScores();
loader.reportFileScoreSet(String path, String outputFilename);
```

However, for any substantial data set, the calculation of scores can require a very long computation time. (For example, 20000 loops means 20000 * 19999 / 2 pairwise calculations, or 199990000 calculations.) For this reason, it is possible to parallelize the calculations. This is done by omitting the call to 'getScores()' and instead following a sequence:

1. Write an intermediate file called a 'comparisons file', which contains all of the pairwise comparisons to be made, with some of them already completed if the overhead on the calculation is low;
2. Calling a method that specifies the 'comparisons file', plus a range of lines that it should read (e.g. 1000000, 2000000), plus an output file name. Each parallel process should read a different section of the comparisons file and write to a different output.
3. Calling a method that reads this collection of output files, which will populate the pair-wise distance matrix
4. Calling `getScores()`, which will use the pre-calculated pairwise distance and return the scores for all the entities

Examples:

Creating the comparison file:

```
LoopSetLoader loader = new LoopSetLoader();
loader.loadLoopSet(path, inputFilename);
String comparisonFile = "pairs";
loader.writeLoopSetComparisonsToFile(path, comparisonFile, 20000000, false);
```

Moving a slice from the comparison file to a file containing the results for that slice; this is called in parallel on multiple processes/machines simultaneously:

```
LoopSet.moveFromPrecalc(tempPath, comparisonFile,       0, 1000000, "fileOut_000.txt");
// Other calls would look like:
//        LoopSet.moveFromPrecalc(path, comparisonFile, 1000000, 1000000, "fileOut_001.txt");
//        LoopSet.moveFromPrecalc(path, comparisonFile, 2000000, 1000000, "fileOut_002.txt");
```

Once all processes are complete, loading the results and generating scores:

```
LoopSetLoader loader = new LoopSetLoader();
loader.loadLoopSet(path, inputFilename);
loader.readLoopSetComparisonsFromFiles(tempPath, new String[]{"fileOut_000.txt", "fileOut_001.txt", "fileOut_002.txt"});
loader.getScores();
loader.reportFileScoreSet(path, "scores.txt");
```
