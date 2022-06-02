package pt.ulusofona.deisi.aed.deisiflix;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class QueryFunctions {
    // Elapsed time variables
    static long startTime = 0;
    static long endTime = 0;

    // Date File Format
    static DateTimeFormatter dateFileFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /* Query useful classes */
    // Class to store data for 'GET_MOVIES_ACTOR_YEAR'
    static class MovieActorYear {
        String title;
        LocalDate date;

        MovieActorYear(String title, LocalDate date) {
            this.title = title;
            this.date = date;
        }
    }

    // Class to store data for 'TOP_MOVIES_WITH_GENDER_BIAS' query
    static class MovieGenderBias {
        String title;
        int discrepancyPercentage;
        char predominantGender;

        MovieGenderBias(String title, int discrepancyPercentage, char predominantGender) {
            this.title = title;
            this.discrepancyPercentage = discrepancyPercentage;
            this.predominantGender = predominantGender;
        }
    }

    // Class to store data for 'GET_TOP_N_YEARS_BEST_AVG_VOTES' query
    static class AVGVotesByYear {
        int year;
        float avgVotes;

        AVGVotesByYear(int year, float avgVotes) {
            this.year = year;
            this.avgVotes = avgVotes;
        }
    }

    /**
     * 'COUNT_MOVIES_ACTOR' Query.
     * Returns the number of movies an actor has been part of. If the actor does not exist, returns 0.
     * @param data Query arguments
     * @param people HashMap (KEY: name, VALUE: MovieAssociate) that stores all people
     * @return Returns the number of movies an actor has participated in. Returns 0 if actors does not exist.
     */
    public static QueryResult countMoviesActor(String data, HashMap<String, MovieAssociate> people) {
        startTime = System.currentTimeMillis();
        String name = data;  // Gets name from data
        int moviesCount = 0;

        // Check if the actor exists
        if (people.containsKey(name)) {
            // Get actor movie count
            moviesCount = people.get(name).associatedMoviesID.size();
        }

        endTime = System.currentTimeMillis();
        String resultValue = "" + moviesCount;
        return new QueryResult(resultValue, (endTime - startTime));
    }

    /**
     * 'GET_MOVIES_ACTOR_YEAR' Query.
     * Returns the movies an actor took part in for a particular year in descending order (by date).
     * @param data Query Arguments
     * @param people HashMap (KEY: name, VALUE: MovieAssociate) that stores all people
     * @param moviesDict HashMap (KEY: movie ID, VALUE: 'Filme' object) with all movies
     * @return Returns all the movies the given actor took part in the given year
     */
    public static QueryResult getMoviesActorYear(String data, HashMap<String, MovieAssociate> people, HashMap<Integer, Filme> moviesDict) {
        startTime = System.currentTimeMillis();
        String[] queryArguments = data.split(" ");

        // Uses 'StringBuilder' to build actor name from query args
        StringBuilder name = new StringBuilder();
        name.append(queryArguments[0]);
        name.append(" ");
        name.append(queryArguments[1]);

        // Gets year from query arguments
        int queryYear = Integer.parseInt(queryArguments[2]);

        // Stores movies the actor participated in the given year
        ArrayList<MovieActorYear> moviesActorYear;
        // ArrayList with all the movies the person has been part of
        ArrayList<Integer> personMovies = people.get(name.toString()).associatedMoviesID;

        moviesActorYear = AuxiliaryQueryFunctions.getMoviesFromYear(queryYear, dateFileFormat, personMovies, moviesDict);

        // Sorts 'moviesActorYear' (using SelectionSort) by Date (in descending order)
        SortingAlgorithms.selSortDateByDescendingOrder(moviesActorYear);

        StringBuilder outputString = new StringBuilder();
        // Sets Date format to be used in the output
        DateTimeFormatter outputDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Builds Output String
        for (int i = 0; i < moviesActorYear.size(); i++) {
            MovieActorYear currentMovie = moviesActorYear.get(i);
            if (i == 0) {
                outputString.append(currentMovie.title);
                outputString.append(" (");
                outputString.append(currentMovie.date.format(outputDateFormat));
                outputString.append(")");
            } else {
                outputString.append("\n");
                outputString.append(currentMovie.title);
                outputString.append(" (");
                outputString.append(currentMovie.date.format(outputDateFormat));
                outputString.append(")");
            }
        }

        endTime = System.currentTimeMillis();
        return new QueryResult(outputString.toString(), (endTime - startTime));
    }

    /**
     * 'COUNT_MOVIES_WITH_ACTORS' Query.
     * Returns the number of movies in which all the given actors appeared simultaneously.
     * @param data Query Arguments
     * @param people HashMap (KEY: name, VALUE: MovieAssociate) that stores all people
     * @param moviesDict HashMap (KEY: movie ID, VALUE: 'Filme' object) with all movies
     * @return Returns the number of movies that contain all the given actors.
     */
    public static QueryResult countMoviesWithActors(String data, HashMap<String, MovieAssociate> people, HashMap<Integer, Filme> moviesDict) {
        startTime = System.currentTimeMillis();

        // Gets actor names from query args
        String[] actors = data.split(";", 2);
        // Gets first actor from actors given args and accesses its 'MovieAssociate' entry in 'people'
        MovieAssociate actor1 = people.get(actors[0]);
        // Stores the other actors separated by ';' (semicolons)
        String otherActors = actors[1];

        // Stores output
        int moviesCount = 0;

        // Iterates through 'actor' movies and checks in how many they all took part in
        for (int movieID : actor1.associatedMoviesID) {
            // Gets current movie being checked
            Filme movie = moviesDict.get(movieID);

            if (movie != null) {
                ArrayList<Pessoa> actorsList = movie.atores;
                // Checks if all actors participated in the movie currently being checked
                if (AuxiliaryQueryFunctions.containsActors(actorsList, otherActors)) {
                    moviesCount++;
                }
            }
        }

        String outputString = String.valueOf(moviesCount);

        endTime = System.currentTimeMillis();
        return new QueryResult(outputString, (endTime - startTime));
    }

    /**
     * 'COUNT_ACTORS_3_YEARS' Query.
     * Returns the number of unique actors that took part in movies in the given years.
     * @param data Query Arguments
     * @param movieIDsByYear HashMap (KEY: year, VALUE: ArrayList with movie IDs) with all movies sorted by year
     * @param moviesDict HashMap (KEY: movie ID, VALUE: 'Filme' object) with all movies
     * @return Returns the unique actors that participated in movies in the given years
     */
    public static QueryResult countActors3Years(
            String data,
            HashMap<Integer, ArrayList<Integer>> movieIDsByYear,
            HashMap<Integer, Filme> moviesDict) {
        startTime = System.currentTimeMillis();
        String[] queryArgs = data.split(" ");  // Gets years from query data

        // Creates an Integer array with query args
        int[] queryYears = new int[3];
        queryYears[0] = Integer.parseInt(queryArgs[0]);
        queryYears[1] = Integer.parseInt(queryArgs[1]);
        queryYears[2] = Integer.parseInt(queryArgs[2]);

        int year1 = queryYears[0];
        int year2 = queryYears[1];
        int year3 = queryYears[2];

        // 'HashSet' to store unique actors' names
        HashSet<String> uniqueActorsYear1 = new HashSet<>();
        HashSet<String> uniqueActorsYear2 = new HashSet<>();
        HashSet<String> uniqueActorsYear3 = new HashSet<>();

        // Adds every actor from each movie in the given year to its correspondent 'HashSet'
        AuxiliaryQueryFunctions.getUniqueMovieActors(year1, movieIDsByYear, uniqueActorsYear1, moviesDict);
        AuxiliaryQueryFunctions.getUniqueMovieActors(year2, movieIDsByYear, uniqueActorsYear2, moviesDict);
        AuxiliaryQueryFunctions.getUniqueMovieActors(year3, movieIDsByYear, uniqueActorsYear3, moviesDict);

        int uniqueActorsCount = 0;

        // Counts unique actors from given years
        for (String actor : uniqueActorsYear1) {
            // Check if 'uniqueActorsYear2' and 'uniqueActorsYear3' also contain the actor, if so, increment counter
            if (uniqueActorsYear2.contains(actor) && uniqueActorsYear3.contains(actor)) {
                uniqueActorsCount++;
            }
        }

        String outputString = String.valueOf(uniqueActorsCount);

        // TODO: Still has margin for improvement

        endTime = System.currentTimeMillis();
        return new QueryResult(outputString, (endTime - startTime));
    }

    /**
     * 'TOP_MOVIES_WITH_GENDER_BIAS' Query.
     * Returns the N (given number) movies with the greatest gender percentual discrepancy in the given years.
     * @param data Query Arguments
     * @param moviesByYear HashMap (KEY: year, VALUE: ArrayList with movie IDs) with all movies sorted by year
     * @param moviesDict HashMap (KEY: movie ID, VALUE: 'Filme' object) with all movies
     * @return Returns the number of movies with the greatest gender percentual discrepancy in the given year
     */
    public static QueryResult topMoviesWithGenderBias(
            String data,
            HashMap<Integer, ArrayList<Integer>> moviesByYear,
            HashMap<Integer, Filme> moviesDict
    ) {
        startTime = System.currentTimeMillis();
        // Gets query args
        String[] queryArgs = data.split(" ");
        // Gets number of movies to output
        int moviesOutNum = Integer.parseInt(queryArgs[0]);
        // Gets year input
        int year = Integer.parseInt(queryArgs[1]);

        ArrayList<MovieGenderBias> moviesGenderBias = new ArrayList<>();

        // Calculates Gender Percentual Discrepancy for all movies in 'year' and adds them to 'moviesGenderBias'
        AuxiliaryQueryFunctions.calculateGenderPercentualDiscrepancy(year, moviesByYear, moviesDict, moviesGenderBias);

        // Sorts 'moviesGenderBias' by Gender Percentual Discrepancy (descending)
        SortingAlgorithms.selSortGenderBiasDescending(moviesGenderBias);

        StringBuilder outputString = new StringBuilder();

        // Builds output string
        for (int i = 0; i < moviesOutNum; i++) {
            MovieGenderBias movie = moviesGenderBias.get(i);

            // Dont add '\n' to the last line
            if (i == moviesOutNum - 1) {
                outputString.append(movie.title);
                outputString.append(':');
                outputString.append(movie.predominantGender);
                outputString.append(':');
                outputString.append(movie.discrepancyPercentage);
            } else {
                outputString.append(movie.title);
                outputString.append(':');
                outputString.append(movie.predominantGender);
                outputString.append(':');
                outputString.append(movie.discrepancyPercentage);
                outputString.append('\n');
            }
        }

        // TODO: Make this more efficient. Still has potential to improve.

        endTime = System.currentTimeMillis();
        return new QueryResult(outputString.toString(), (endTime - startTime));
    }

    public static QueryResult getRecentTitlesSameAVGVotesOneSharedActor(String data, HashMap<Integer, Filme> moviesDict, HashMap<Integer, ArrayList<Integer>> moviesByYear) {
        startTime = System.currentTimeMillis();
        // TODO: Do this query properly. This code looks like SHIT xP
        // Gets query args
        int id = Integer.parseInt(data);

        // Get movie with query id
        Filme movie = moviesDict.get(id);
        float avgVotes = movie.mediaVotos;
        LocalDate date = LocalDate.parse(movie.dataLancamento, dateFileFormat);
        ArrayList<Pessoa> actors = movie.atores;

        // Stores valid movie titles
        ArrayList<String> validMovieTitles = new ArrayList<>();

        // Checks all the movies in the same year or older
        for (int year = date.getYear(); year < 2022; year++) {
            if (moviesByYear.containsKey(year)) {
                // Check each movie in the current year and see if average votes match the given one
                for (int movieID : moviesByYear.get(year)) {
                    Filme currentMovie = moviesDict.get(movieID);
                    int sharedActors = 0;

                    // If year is the same we have to check if movie is older than the given one
                    if (year == date.getYear() && LocalDate.parse(currentMovie.dataLancamento, dateFileFormat).isAfter(date)) {
                        if (currentMovie.mediaVotos == avgVotes && currentMovie.atores != null) {
                            // Checks if has only one shared actor
                            for (Pessoa actor : currentMovie.atores) {
                                if (actors.contains(actor)) {
                                    sharedActors++;
                                }
                            }
                        }
                    } else {
                        if (currentMovie.mediaVotos == avgVotes && currentMovie.atores != null) {
                            // Checks if has only one shared actor
                            for (Pessoa actor : currentMovie.atores) {
                                if (actors.contains(actor)) {
                                    sharedActors++;
                                }
                            }
                        }
                    }

                    // Checks if theres only one shared actor
                    if (sharedActors > 0) {
                        // Adds movie title to 'validMovieTitles'
                        validMovieTitles.add(currentMovie.titulo);
                    }
                }
            }
        }

        StringBuilder outputString = new StringBuilder();
        for (int i = 0; i < validMovieTitles.size(); i++) {
             outputString.append(validMovieTitles.get(i));
             if (i != validMovieTitles.size() - 1) {
                 outputString.append("||");
             }
        }


        endTime = System.currentTimeMillis();
        return new QueryResult(outputString.toString(), (endTime - startTime));
    }

    /**
     * 'GET_TOP_N_YEARS_BEST_AVG_VOTES' Query.
     * Returns the N (given number) of years with the best movie average votes between all movies.
     * @param data Query Arguments
     * @param moviesByYear HashMap (KEY: year, VALUE: ArrayList with movie IDs) with all movies sorted by year
     * @param moviesDict HashMap (KEY: movie ID, VALUE: 'Filme' object) with all movies
     * @return Returns the years with the best average votes
     */
    public static QueryResult getTopNYearsBestAVGVotes(String data, HashMap<Integer, ArrayList<Integer>> moviesByYear, HashMap<Integer, Filme> moviesDict) {
        startTime = System.currentTimeMillis();
        // Gets number of movies to output
        int moviesOutputNum = Integer.parseInt(data);

        ArrayList<AVGVotesByYear> votesByYear = new ArrayList<>();

        // Adds each year votes average to 'votesByYear'
        for (Integer movie : moviesByYear.keySet()) {
            float yearVotesAverage = AuxiliaryQueryFunctions.calculateYearVotesAverage(moviesByYear.get(movie), moviesDict);
            votesByYear.add(new AVGVotesByYear(movie, yearVotesAverage));
        }

        // Sorts 'votesByYear' by average votes (by ascending order)
        SortingAlgorithms.quickSortByAVGVotes(votesByYear);

        StringBuilder outputString = new StringBuilder();
        // Builds output string: starts at the last 'votesByYear' index because it is sorted in ascending order
        for (int pos = votesByYear.size() - 1, i = 0; i < moviesOutputNum; pos--, i++) {
            // Gets current 'AVGVotesByYear' object
            AVGVotesByYear current = votesByYear.get(pos);

            // Appends data to 'outputString'
            outputString.append(current.year);
            outputString.append(':');
            outputString.append(Math.round(current.avgVotes * 100) / 100.0);

            // Does not put '\n' in the last year
            if (i != moviesOutputNum - 1) {
                outputString.append('\n');
            }
        }

        endTime = System.currentTimeMillis();
        return new QueryResult(outputString.toString(), (endTime - startTime));
    }

    public static QueryResult distanceBetweenActors(String data) {
        startTime = System.currentTimeMillis();
        // TODO
        endTime = System.currentTimeMillis();
        return new QueryResult();
    }

    public static QueryResult getTopNMoviesRatio(String data) {
        startTime = System.currentTimeMillis();
        // TODO
        endTime = System.currentTimeMillis();
        return new QueryResult();
    }

    public static QueryResult top6DirectorsWithinFamily(String data) {
        startTime = System.currentTimeMillis();
        // TODO
        endTime = System.currentTimeMillis();
        return new QueryResult();
    }

    public static QueryResult getTopActorYear(String data) {
        startTime = System.currentTimeMillis();
        // TODO
        endTime = System.currentTimeMillis();
        return new QueryResult();
    }

    public static QueryResult insertActor(String data) {
        startTime = System.currentTimeMillis();
        // TODO
        endTime = System.currentTimeMillis();
        return new QueryResult();
    }

    public static QueryResult removeActor(String data) {
        startTime = System.currentTimeMillis();
        // TODO
        endTime = System.currentTimeMillis();
        return new QueryResult();
    }

    public static QueryResult getDuplicateLinesYear(String data) {
        startTime = System.currentTimeMillis();
        // TODO
        endTime = System.currentTimeMillis();
        return new QueryResult();
    }
}
