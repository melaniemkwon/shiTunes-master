import java.sql.*;
import java.util.ArrayList;

/**
 * The ShiBase class contains methods for connecting to,
 * building and interacting with the shiTunes database
 *
 * @author shiTunes inc.
 */
public class ShiBase {

    private static final String DB_NAME = "ShiBase";
    private static final String SONG_TABLE = "SONG";
    private static final String PLAYLIST_TABLE = "PLAYLIST";
    private static final String PLAYLIST_SONG_TABLE = "PLAYLIST_SONG";
    private static final String COLUMN_CONFIG_TABLE = "COLUMN_CONFIG";
    private static final String RECENT_SONGS_TABLE = "RECENT_SONGS";
    private static final String[] SONG_COLUMNS =  {"songId", "filePath", "title", "artist", "album", "yearReleased",
            "genre", "comment"};
    private static final String[] PLAYLIST_COLUMNS = {"playlistId", "playlistName"};
    private static final String[] PLAYLIST_SONG_COLUMNS = {"playlistId", "songId"};
    private static final String[] COLUMN_CONFIG_COLUMNS = {"columnName", "columnIndex", "columnVisible"};
    private static final String[] RECENT_SONGS_COLUMNS = {"songId"};
    private static final String CREATE = ";create=true";
    private static final String PROTOCOL = "jdbc:derby:";
    private Connection conn;
    private PreparedStatement stmt;
    private boolean connected;

    /**
     * The ShiBase default constructor
     * <p>
     * Connects to the database, if not already connected &
     * creates tables, if not already created
     */
    public ShiBase() {
        connect();      // creates db if not already present
        createTables(); // if not already present
    }

    /* ************************ */
    /* ************************ */
    /* GENERAL DATABASE METHODS */
    /* ************************ */
    /* ************************ */

    /*
     * Connects to the database, creates database if not already
     * created
     *
     */
    private void connect() {
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
            //Get a connection
            conn = DriverManager.getConnection(PROTOCOL + DB_NAME);
            // getConnection() can also have a second parameter, Properties,  to add username/password etc
            connected = true;
        } catch (Exception except) {
            // If database does not exist; create database
            createDatabase();
        }
    }

    /*
     * Creates ShiBase database
     *
     * @return true if database was created successfully
     */
    private boolean createDatabase() {
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
            //Get a connection
            conn = DriverManager.getConnection(PROTOCOL + DB_NAME + CREATE);
            // getConnection() can also have a second parameter, Properties,  to add username/password etc
            connected = true;
            return true;
        } catch (Exception except) {
            except.printStackTrace();
            return false;
        }
    }

    /*
     * Creates the database tables, if not already present
     */
    private void createTables() {
        createSongTable();
        createPlaylistTable();
        createPlaylistSongTable();
        createColumnConfigTable();
        createRecentSongTable();
    }

    /**
     * Checks if database is connected
     *
     * @return true if database is connected
     */
    public boolean isConnected() {
        return connected;
    }


    /* ******************* */
    /* ******************* */
    /* SONG TABLE  METHODS */
    /* ******************* */
    /* ******************* */

    /*
     * Creates SONG table, if it doesn't already exist
     *
     * @return true if table was created successfully
     */
    private boolean createSongTable() {
        try {
            String query = "CREATE TABLE " + SONG_TABLE +
                    " (songId INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                    "filePath VARCHAR(200) UNIQUE NOT NULL, " +
                    "title VARCHAR(150), " +
                    "artist VARCHAR(100), " +
                    "album VARCHAR(150), " +
                    "yearReleased VARCHAR(4), " +
                    "genre VARCHAR(20), " +
                    "comment VARCHAR(200), " +
                    "PRIMARY KEY (songId))";
            stmt = conn.prepareStatement(query);
            stmt.execute();
            stmt.close();
            return true;
        }
        catch (SQLException sqlExcept) {
            // Table Exists
        }
        return false;
    }

    /**
     * Inserts the given song into the ShiBase database
     *
     * @param song the song to insert into the database
     * @return the song id in db if the song was inserted successfully
     *         -1 if the song already exists, or the insert failed
     */
    public int insertSong(Song song) {
        // To store the song id, or return -1 if db insert fails
        int id = -1;
        ResultSet keys = null;

        if(!songExists(song.getFilePath())) {
            try {
                String query = "INSERT INTO " + SONG_TABLE +
                        " (filePath, title, artist, album, yearReleased, genre, comment)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?)";
                stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, song.getFilePath());
                stmt.setString(2, song.getTitle());
                stmt.setString(3, song.getArtist());
                stmt.setString(4, song.getAlbum());
                stmt.setString(5, song.getYear());
                stmt.setString(6, song.getGenre());
                stmt.setString(7, song.getComment());
                stmt.execute();
                keys = stmt.getGeneratedKeys();
                while (keys.next()) {
                    id = keys.getInt(1);
                }
                keys.close();
                stmt.close();
            } catch (SQLException sqlExcept) {
                sqlExcept.printStackTrace();
            }
        }
        return id;
    }

    /**
     * Checks if song exists in database
     *
     * @param filePath the filePath of the song to look for in the database
     * @return true if the song exists in the database
     */
    public boolean songExists(String filePath) {
        int rowCount = 0;

        try {
            String query = "SELECT count(*) AS rowcount FROM " + SONG_TABLE +
                    " WHERE filePath=?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, filePath);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            rowCount = resultSet.getInt("rowcount");
            stmt.close();
            if(rowCount != 0) {
                // song exists, return true
                return true;
            }
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
            return false;
        }
        // song doesn't exist
        return false;
    }

    /**
     * Deletes a given song from the database
     *
     * @param songId the unique song id of the song to delete
     * @return true if the song was successfully deleted, false if otherwise
     */
    public boolean deleteSong(int songId) {
        try {
            String query = "DELETE FROM " + SONG_TABLE + " WHERE songId=?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, songId);
            stmt.execute();
            stmt.close();
            return true;
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return false;
    }

    /**
     * Gets all songs from the database
     *
     * @return all songs from the database as a multidimensional object array
     */
    public Object[][] getAllSongs()
    {
        Object[][] allSongs;
        int rowCount = 0;
        int index = 0;

        try {
            // Get record count
            String rowCountQuery = "SELECT count(*) AS rowcount FROM " + SONG_TABLE;
            stmt = conn.prepareStatement(rowCountQuery);
            ResultSet rowCountRS = stmt.executeQuery();
            rowCountRS.next();
            rowCount = rowCountRS.getInt("rowcount");

            // Initialize multidimensional array large enough to hold all songs
            allSongs = new Object[rowCount][SONG_COLUMNS.length];

            // Get all records
            String allSongsQuery = "SELECT * FROM " + SONG_TABLE + " ORDER BY title";
            stmt = conn.prepareStatement(allSongsQuery);
            ResultSet allSongsRS = stmt.executeQuery();

            while(allSongsRS.next()) {
                allSongs[index] = getSongRow(allSongsRS);
                index++;
            }
            stmt.close();
            return allSongs;
        }
        catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return new Object[0][0];
    }

    /**
     * Get the unique integer id of a song based on its
     * file path (which is also unique)
     *
     * @param filePath the filepath of the song being searched for
     * @return the unique integer id of the song being searched for
     *         returns -1 if not found
     */
    public int getSongId(String filePath) {
        int songId = -1;
        try {
            String query = "SELECT * FROM " + SONG_TABLE + " WHERE filePath=?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, filePath);
            ResultSet songIdRS = stmt.executeQuery();
            if(songIdRS.next()) {
                songId = songIdRS.getInt("songId");
            }
            stmt.close();
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return songId;
    }

    /**
     * Get the unique file path of a song based on its
     * song id (which is also unique)
     *
     * @param songId the song id of the song being searched for
     * @return the unique file path of the song being searched for
     */
    public String getSongFilePath(int songId) {
        String songFilePath = null;
        try {
            String query = "SELECT * FROM " + SONG_TABLE + " WHERE songId=?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, songId);
            ResultSet songIdRS = stmt.executeQuery();
            if(songIdRS.next()) {
                songFilePath = songIdRS.getString("filePath");
            }
            stmt.close();
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return songFilePath;
    }


    /**
     * Get the song title of a song based on its
     * song id
     *
     * @param songId the song id of the song being searched for
     * @return the song title of the song being searched for
     */
    public String getSongTitle(int songId) {
        String title = null;
        try {
            String query = "SELECT title FROM " + SONG_TABLE + " WHERE songId=?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, songId);
            ResultSet songIdRS = stmt.executeQuery();
            if(songIdRS.next()) {
                title = songIdRS.getString("title");
            }
            stmt.close();
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return title;
    }

    /*
     * Returns the given SONG row as a String array
     *
     * @param rs the current result set item
     * @return the given result from the SONG table as a String array
     */
    private String[] getSongRow(ResultSet rs) {
        String[] song = new String[SONG_COLUMNS.length];
        try {
            for(int i = 0; i < SONG_COLUMNS.length; i++) {
                song[i] = rs.getString(SONG_COLUMNS[i]);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return song;
    }

    /* ********************** */
    /* ********************** */
    /* PLAYLIST TABLE METHODS */
    /* ********************** */
    /* ********************** */

    /*
     * Create the PLAYLIST table
     *
     * @return true if table created successfully
     */
    private boolean createPlaylistTable() {
        try {
            String query = "CREATE TABLE " + PLAYLIST_TABLE +
                    " (playlistId INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                    "playlistName VARCHAR(100) UNIQUE NOT NULL, " +
                    "PRIMARY KEY (playlistId))";
            stmt = conn.prepareStatement(query);
            stmt.execute();
            stmt.close();
            return true;
        }
        catch (SQLException sqlExcept) {
            // Table Exists
        }
        return false;
    }

    /**
     * Accessor method to get all playlist names
     *
     * @return an ArrayList of playlist names as Strings
     */
    public ArrayList<String> getPlaylistNames() {
        ArrayList<String> playlistNames = new ArrayList<String>();
        try {
            // Get all playlist names
            String query = "SELECT playlistName FROM " + PLAYLIST_TABLE +
                    " ORDER BY playlistName ASC";
            stmt = conn.prepareStatement(query);
            ResultSet playlistRS = stmt.executeQuery();
            while(playlistRS.next()) {
                playlistNames.add(playlistRS.getString("playlistName"));
            }
            stmt.close();
        }
        catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return playlistNames;
    }

    /**
     * Add a new playlist to the PLAYLIST table
     *
     * @param playlist the name of the newly created playlist
     * @return true if entry successfully added to table
     */
    public boolean addPlaylist(String playlist) {
        try {
            String query = "INSERT INTO " + PLAYLIST_TABLE + " (playlistName) VALUES (?)";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, playlist);
            stmt.execute();
            stmt.close();

            //playlistNames.add(playlist);
        }
        catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Delete a playlist from the PLAYLIST table
     *
     * @param playlist the name of playlist to be deleted
     * @return true if entry successfully deleted from table
     */
    public boolean deletePlaylist(String playlist) {
        try {
            String query = "DELETE FROM " + PLAYLIST_TABLE +
                    " WHERE playlistName = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, playlist);
            stmt.execute();
            stmt.close();

            //playlistNames.remove(playlist);
            return true;
        }
        catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return false;
    }

    /**
     * Adds the given song to given playlist
     *
     * @param filePath the filePath of the song being added
     * @param playlistName the name of the playlist to add the given song to
     * @return true if song successfully added to playlist
     */
    public boolean addSongToPlaylist(String filePath, String playlistName) {
        try {
            int songId = getSongId(filePath);
            int playlistId = getPlaylistId(playlistName);
            if(songId!= -1 && playlistId != -1) {
                // SUCCESS: song and playlist id's found
                String query = "INSERT INTO " + PLAYLIST_SONG_TABLE +
                        " (playlistId, songId) " +
                        " VALUES (" + playlistId + ", " + songId + ")";
                stmt = conn.prepareStatement(query);
                stmt.execute();
                stmt.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Deletes the given song to given playlist
     *
     * @param songId the unique song id of the song being deleted
     * @param playlist the playlist to delete the given song from
     * @return true if song successfully deleted to playlist
     */
    public boolean deleteSongFromPlaylist(int songId, String playlist) {
        try {
            int playlistId = getPlaylistId(playlist);
            if(songId!= -1 && playlistId != -1) {
                // SUCCESS: song and playlist id's found
                String query = "DELETE FROM " + PLAYLIST_SONG_TABLE +
                        " WHERE playlistId = " + playlistId +
                        " AND songId = " + songId;
                stmt = conn.prepareStatement(query);
                stmt.execute();
                stmt.close();
                return true;
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     * Create the junction table that will associate a Song with
     * a Playlist by Primary Key (id)
     *
     * @return true if table created successfully
     */
    private boolean createPlaylistSongTable() {
        try {
            String query = "CREATE TABLE " + PLAYLIST_SONG_TABLE +
                    "(playlistId INTEGER NOT NULL, " +
                    "songId INTEGER NOT NULL, " +
                    "CONSTRAINT fk_songId FOREIGN KEY (songId) " +
                    "REFERENCES " + SONG_TABLE + " (songId) " +
                    "ON DELETE CASCADE, " +
                    "CONSTRAINT fk_playlistId FOREIGN KEY (playlistId) " +
                    "REFERENCES " + PLAYLIST_TABLE + " (playlistId) " +
                    "ON DELETE CASCADE )";
            stmt = conn.prepareStatement(query);
            stmt.execute();
            stmt.close();
            return true;
        }
        catch (SQLException sqlExcept) {
            // Table Exists
        }
        return false;
    }

    /**
     * Get all the songs associated with the given playlistId
     *
     * @param playlistName the name of the playlist to get all songs from
     * @return an ArrayList of Songs associated with the given playlist
     */
    public Object[][] getPlaylistSongs(String playlistName) {
        Object[][] playlistSongs;
        int playlistId = getPlaylistId(playlistName);
        int rowCount;
        int index = 0;

        try {
            // Get record count - which will be the size of
            // the first dimension of the multidimensional array
            // this method returns (ie. the number of songs in playlist)
            String rowCountQuery = "SELECT count(*) AS rowcount FROM " + PLAYLIST_SONG_TABLE +
                    " WHERE playlistId = " + playlistId;
            stmt = conn.prepareStatement(rowCountQuery);
            ResultSet rowCountRS = stmt.executeQuery();
            rowCountRS.next();
            rowCount = rowCountRS.getInt("rowcount");

            // Initialize multidimensional array large enough to hold all songs in playlist
            playlistSongs = new Object[rowCount][SONG_COLUMNS.length];

            // Get all playlist songs
            String query = "SELECT * FROM " + SONG_TABLE +
                    " JOIN " + PLAYLIST_SONG_TABLE +
                    " USING (songId) WHERE playlistID = " + playlistId +
                    " ORDER BY title";
            stmt = conn.prepareStatement(query);
            ResultSet playlistSongsRS = stmt.executeQuery();

            while(playlistSongsRS.next()) {
                playlistSongs[index] = getSongRow(playlistSongsRS);
                index++;
            }
            stmt.close();
            return playlistSongs;
        }
        catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return new Object[0][0];
    }

    /*
     * Get the unique integer id of a playlist based on its
     * name (which is also unique)
     *
     * @param playlistName the name of the playlist being searched for
     * @return the unique integer id of the playlist being searched for
     *         returns -1 if not found
     */
    private int getPlaylistId(String playlistName) {
        int playlistId = -1;
        try {
            String query = "SELECT * FROM " + PLAYLIST_TABLE + " WHERE playlistName=?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, playlistName);
            ResultSet songIdRS = stmt.executeQuery();
            if(songIdRS.next()) {
                playlistId = songIdRS.getInt("playlistId");
            }
            stmt.close();
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return playlistId;
    }


    /* ********************** */
    /* ********************** */
    /* COLUMN TABLE METHODS   */
    /* ********************** */
    /* ********************** */

    /*
     * Creates COLUMN_CONFIG table, if it doesn't already exist
     *
     * @return true if table was created successfully
     */
    private boolean createColumnConfigTable() {
        String query;
        try {
            // Create Table
            query = "CREATE TABLE " + COLUMN_CONFIG_TABLE +
                    " (columnName VARCHAR(50)," +
                    "columnVisible BOOLEAN NOT NULL)";
            stmt = conn.prepareStatement(query);
            stmt.execute();
            stmt.close();

            // Populate table with default values
            for(int i = 0; i < MusicTable.SONG_COLUMN_NAMES.length; i++) {
                query = "INSERT INTO " + COLUMN_CONFIG_TABLE +
                        " (columnName, columnVisible)" +
                        " VALUES (?, ?)";
                stmt = conn.prepareStatement(query);
                String columnName = MusicTable.SONG_COLUMN_NAMES[i];
                stmt.setString(1, columnName);
                if (columnName.equals("ID") || columnName.equals("File Path")) {
                    stmt.setBoolean(2, false);  // default state for ID & File Path is !visible
                } else {
                    stmt.setBoolean(2, true);   // default state for all other columns is visible
                }
                stmt.execute();
                stmt.close();
            }
            return true;
        } catch (SQLException sqlExcept) {
            // Table Exists
        }
        return false;
    }

    /**
     * Accessor method to get a columns visibility
     *
     * @return boolean indidcating whether column is visible
     * @param columnName the column name to search for
     */
    public boolean getColumnVisible(String columnName) {
        boolean columnVisible = false;
        try {
            // Get all playlist names
            String query = "SELECT columnVisible FROM " + COLUMN_CONFIG_TABLE +
                    " WHERE columnName=?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, columnName);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            columnVisible = resultSet.getBoolean("columnVisible");
            stmt.close();
        }
        catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
        return columnVisible;
    }

    /**
     * Mutator method to set a columns visibility
     *
     * @return boolean indicating whether column is visible
     * @param columnName the column name of the column being changed
     * @param visible the visible to set for the column
     */
    public void setColumnVisible(String columnName, boolean visible) {
        try {
            // Get all playlist names
            String query = "UPDATE " + COLUMN_CONFIG_TABLE +
                    " SET columnVisible=? " +
                    " WHERE columnName=?";
            stmt = conn.prepareStatement(query);
            stmt.setBoolean(1, visible);
            stmt.setString(2, columnName);
            stmt.execute();
            stmt.close();
        }
        catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
    }

    /* ******************** */
    /* ******************** */
    /* RECENT SONG  METHODS */
    /* ******************** */
    /* ******************** */

    /*
     * Creates RECENT_SONGS table, if it doesn't already exist
     *
     * @return true if table was created successfully
     */
    private boolean createRecentSongTable() {
        try {
            String query = "CREATE TABLE " + RECENT_SONGS_TABLE +
                    " (songId INTEGER NOT NULL, " +
                    "CONSTRAINT fk_recent_songId FOREIGN KEY (songId) " +
                    "REFERENCES " + SONG_TABLE + " (songId) " +
                    "ON DELETE CASCADE)";
            stmt = conn.prepareStatement(query);
            stmt.execute();
            stmt.close();
            return true;
        } catch (SQLException sqlExcept) {
            // Table Exists
        }
        return false;
    }

    /**
     * Adds the given song to recent songs
     *
     * @param songId the song to add to recent songs
     * @return true if song successfully added to recent songs
     */
    public boolean addRecentSong(int songId) {
        try {
            // Insert given song into recent songs table
            String query = "INSERT INTO " + RECENT_SONGS_TABLE +
                    " (songId) " +
                    " VALUES (?)";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, songId);
            stmt.execute();
            stmt.close();

            // Get row count after insert
            query = "SELECT count(*) as rowCount FROM " + RECENT_SONGS_TABLE;
            stmt = conn.prepareStatement(query);
            ResultSet countRS = stmt.executeQuery();
            int rowCount = 0;
            while(countRS.next()) {
                rowCount = countRS.getInt("rowCount");
            }
            stmt.close();

            // If rowCount > 10 (ie. 11) delete the oldest song in recent songs table
            if(rowCount > 10) {
                query = "DELETE FROM " + RECENT_SONGS_TABLE +
                        " WHERE songId IN (SELECT songId FROM " + RECENT_SONGS_TABLE +
                        " FETCH FIRST ROW ONLY)";
                stmt = conn.prepareStatement(query);
                stmt.executeUpdate();
                stmt.close();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get all recent song ids
     *
     * @return array of recent song ids
     */
    public int[] getRecentSongs() {
        ArrayList<Integer> recentSongsList = new ArrayList<Integer>();
        try {
            // Get all recent song ids
            String query = "SELECT songId FROM " + RECENT_SONGS_TABLE;
            stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                recentSongsList.add(rs.getInt("songId"));
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int[] recentSongs = new int[recentSongsList.size()];
        for(int i = 0; i < recentSongs.length; i++) {
            recentSongs[i] = recentSongsList.get(i);
        }
        return recentSongs;
    }
}