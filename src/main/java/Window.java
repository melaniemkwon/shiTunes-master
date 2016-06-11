import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;


/**
 * The Window class builds the shiTunes Graphical User Interface
 * and can be instantiated as either a full application window
 * by calling the default constructor, or as a playlist only window
 * which does not include the playlist navigation panel by calling
 * the overloaded constructor Window(playlistName)
 *
 * @author shiTunes inc.
 */
public class Window
       extends JFrame
       implements BasicPlayerListener {

    /*
     * Indicates the player state
     * <p>
     * State Codes:
     * <p>
     * -1: UNKNOWN
     * 0: OPENING
     * 1: OPENED
     * 2: PLAYING
     * 3: STOPPED
     * 4: PAUSED
     * 5: RESUMED
     * 6: SEEKING
     * 7: SEEKED
     * 8: EOM
     * 9: PAN
     * 10: GAIN
     *
     */
    private int playerState;
    private int windowType;
    private static int MAIN = 0;
    private static int PLAYLIST = 1;
    private JFrame windowFrame;
    private JScrollPane musicTableScrollPane;
    private MusicTable musicTable;
    private JPopupMenu musicTablePopupMenu;
    private JMenu addSongToPlaylistSubMenu;
    private MusicTablePopupListener musicTablePopupListener = new MusicTablePopupListener();
    private ColumnDisplayPopupListener columnDisplayPopupListener = new ColumnDisplayPopupListener();
    private JPopupMenu playlistPopupMenu;
    private JPopupMenu showColumnsPopupMenu;
    private JTree playlistPanelTree;
    private DefaultMutableTreeNode playlistNode;
    private String selectedPlaylist;
    private MusicPlayer player;
    private JSlider volumeSlider;
    private JMenu playRecentSubMenu;
    private JProgressBar progressBar;
    private JLabel leftTimer;
    private JLabel rightTimer;
    private long timeRemaining;
    private long timeElapsed;
    private int duration;
    private boolean songCompleted;
    private JCheckBoxMenuItem shuffleItem;
    private JCheckBoxMenuItem repeatItem;



    /**
     * The Window default constructor
     * <p>
     * Builds the shiTunes main application window
     */
    public Window() {

        // Set this Window instance's type to Window.MAIN
        this.windowType = Window.MAIN;

        // Set this Window instance's table
        this.musicTable = new MusicTable();

        // Set selected playlist to "Library" (the default table in Window.MAIN)
        this.selectedPlaylist = "Library";

        // Set this Window instance's player
        player = new MusicPlayer();
        player.getPlayer().addBasicPlayerListener(this);

        buildWindowLayout("shiTunes");
    }

    /**
     * The Window overloaded constructor
     * <p>
     * Builds a shiTunes playlist window
     *
     * @param playlistName the name of the Playlist
     */
    public Window(String playlistName) {

        // Set this Window instance's type to Window.PLAYLIST
        this.windowType = Window.PLAYLIST;

        // Set selected playlist
        this.selectedPlaylist = playlistName;

        // Set this Window instance's table
        this.musicTable = new MusicTable(playlistName);

        // Set this Window instance's player
        player = new MusicPlayer();
        player.getPlayer().addBasicPlayerListener(this);

        // Add this window to list of application windows
        ShiTunes.windows.add(this);

        buildWindowLayout(playlistName);
    }

    /**
     * Displays the window
     *
     */
    public void display() {
        windowFrame.setVisible(true);
    }

    /**
     * Builds the Window's layout based on it's type
     *
     * @param windowTitle the title of the window
     */
    private void buildWindowLayout(String windowTitle) {
        // Create outer shiTunes frame and set various parameters
        windowFrame = new JFrame();
        windowFrame.setTitle(windowTitle);
        windowFrame.setMinimumSize(new Dimension(900, 600));
        windowFrame.setLocationRelativeTo(null);

        if(windowType == Window.MAIN) {
            windowFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        } else {
            if (windowType == Window.PLAYLIST) {
                windowFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                windowFrame.addWindowListener(new PlaylistWindowListener());
            }
        }

        // Create the main panel that resides within the windowFrame
        // Layout: BoxLayout, X_AXIS
        JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainPanel.setDividerLocation(150);

        // Instantiate scroll pane for table
        musicTableScrollPane = new JScrollPane(musicTable.getTable());

        // Create the controlTablePanel that will reside within the mainPanel
        // Layout: BoxLayout, Y_AXIS
        JPanel controlTablePanel = new JPanel();
        controlTablePanel.setLayout(new BoxLayout(controlTablePanel, BoxLayout.Y_AXIS));
        controlTablePanel.add(getControlPanel());
        controlTablePanel.add(musicTableScrollPane);
        controlTablePanel.setMinimumSize(new Dimension(500, 600));

        // Create menuBar and add File/Control menus
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(getFileMenu());
        menuBar.add(getControlsMenu());

        // Build the music table
        buildMusicTable();

        //creates the right click menu for the playlists
        createPlaylistPopupMenu();

        // Build main panel
        if(windowType == Window.MAIN) {
            mainPanel.add(getPlaylistPanel());
        }
        mainPanel.add(controlTablePanel);

        // Add all GUI components to shiTunes application frame
        windowFrame.setJMenuBar(menuBar);
        windowFrame.setContentPane(mainPanel);
        // windowFrame.setIconImage();
        windowFrame.pack();
        windowFrame.setLocationByPlatform(true);
    }

    /**
     * Builds the music table:
     * <ul>
     * <li>Sets various viewport parameters</li>
     * <li>Assigns listeners</li>
     * </ul>
     *
     */
    private void buildMusicTable() {
        musicTable.getTable().setPreferredScrollableViewportSize(new Dimension(500, 200));
        musicTable.getTable().setFillsViewportHeight(true);

        /* Add listeners */

        // Create right-click popup menu and set popup listener up JTable
        createMusicTablePopupMenu();
        musicTable.getTable().addMouseListener(musicTablePopupListener);

        //creates the right click menu for column displays
        createShowColumnsPopupMenu();
        musicTable.getTable().getTableHeader().addMouseListener(columnDisplayPopupListener);

        // Add double click listener to play selected song.
        musicTable.getTable().addMouseListener(new DoubleClickListener());

        // Add drop target on table
        // enabling drag and drop of files into table
        musicTable.getTable().setDropTarget(new AddToTableDropTarget());
    }

    /**
     * Returns the playlist panel
     *
     * @return the playlist panel containing the Library and Playlist branches
     */
    private JScrollPane getPlaylistPanel() {
        // Create the root node
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        // Create library and playlist nodes
        DefaultMutableTreeNode libraryNode = new DefaultMutableTreeNode ("Library");
        playlistNode = new DefaultMutableTreeNode ("Playlists");

        updatePlaylistNode();

        // Add library and playlist nodes to the root
        root.add(libraryNode);
        root.add(playlistNode);

        // Create playlist panel tree
        playlistPanelTree = new JTree(root);

        // Add mouse listener: manages left and right click
        playlistPanelTree.addMouseListener(new PlaylistPanelMouseListener());

        // Make the root node invisible
        playlistPanelTree.setRootVisible(false);

        // Expand playlist node (index 1)
        playlistPanelTree.expandRow(1);

        // Set Icons
        try {
            BufferedImage musicResource = ImageIO.read(getClass().getResourceAsStream("/images/music.png"));
            BufferedImage plusResource = ImageIO.read(getClass().getResourceAsStream("/images/plus.png"));
            BufferedImage minusResource = ImageIO.read(getClass().getResourceAsStream("/images/minus.png"));
            ImageIcon music = new ImageIcon(musicResource);
            ImageIcon plus = new ImageIcon(plusResource);
            ImageIcon minus = new ImageIcon(minusResource);
            DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
            renderer.setOpenIcon(minus);
            renderer.setClosedIcon(plus);
            renderer.setLeafIcon(music);
            playlistPanelTree.setCellRenderer(renderer);
        } catch(IOException e) {
            e.printStackTrace();
        }

        // Instantiate playlist panel pane to be returned
        // and set minimum dimensions
        JScrollPane playlistPanelPane = new JScrollPane(playlistPanelTree);
        playlistPanelPane.setMinimumSize(new Dimension(150, 600));

        return playlistPanelPane;
    }

    /**
     * Updates the playlist node in the playlist panel tree
     * with the most up to date list of playlist names from the database
     *
     */
    private void updatePlaylistNode(){
        ArrayList<String> playlistNames = ShiTunes.db.getPlaylistNames();

        playlistNode.removeAllChildren();

        for(String playlistName : playlistNames) {
            DefaultMutableTreeNode playlist = new DefaultMutableTreeNode(playlistName);

            playlistNode.add(playlist);
        }
    }

    /**
     * Adds UI Components to shiTunes Window
     *
     * @return the control JPanel
     */
    private JPanel getControlPanel() {
        // Instantiate the controlPanel (Buttons and Volume Slider)
        JPanel controlPanel = new JPanel();
        try {
            // Initialize resources
            BufferedImage playResource = ImageIO.read(getClass().getResourceAsStream("/images/play.png"));
            BufferedImage pauseResource = ImageIO.read(getClass().getResourceAsStream("/images/pause.png"));
            BufferedImage stopResource = ImageIO.read(getClass().getResourceAsStream("/images/stop.png"));
            BufferedImage previousResource = ImageIO.read(getClass().getResourceAsStream("/images/previous.png"));
            BufferedImage nextResource = ImageIO.read(getClass().getResourceAsStream("/images/next.png"));
            ImageIcon stopIcon = new ImageIcon(stopResource);
            ImageIcon pauseIcon = new ImageIcon(pauseResource);
            ImageIcon playIcon = new ImageIcon(playResource);
            ImageIcon previousIcon = new ImageIcon(previousResource);
            ImageIcon nextIcon = new ImageIcon(nextResource);

            // Initialize buttons (toggle play/pause, stop, previous, next)
            // Setting icon during intialization
            JButton playButton = new JButton(playIcon);
            JButton pauseButton = new JButton(pauseIcon);
            JButton stopButton = new JButton(stopIcon);
            JButton previousButton = new JButton(previousIcon);
            JButton nextButton = new JButton(nextIcon);

            // Initialize Volume Slider
            volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
            volumeSlider.setMinorTickSpacing(10);
            volumeSlider.setMajorTickSpacing(20);
            volumeSlider.setPaintTicks(false);
            volumeSlider.setPaintLabels(false);
            volumeSlider.setLabelTable(volumeSlider.createStandardLabels(10));

            // Set preferred button size
            playButton.setPreferredSize(new Dimension(40, 40));
            pauseButton.setPreferredSize(new Dimension(40, 40));
            stopButton.setPreferredSize(new Dimension(40, 40));
            previousButton.setPreferredSize(new Dimension(40, 40));
            nextButton.setPreferredSize(new Dimension(40, 40));

            // Set action listeners
            playButton.addActionListener(new PlayListener());
            pauseButton.addActionListener(new PauseListener());
            stopButton.addActionListener(new StopListener());
            previousButton.addActionListener(new PreviousListener());
            nextButton.addActionListener(new NextListener());
            volumeSlider.addChangeListener(new VolumeSliderListener());

            // Add buttons to controlPanel
            controlPanel.add(getProgressBar());
            controlPanel.add(previousButton);
            controlPanel.add(playButton);
            controlPanel.add(pauseButton);
            controlPanel.add(stopButton);
            controlPanel.add(nextButton);
            controlPanel.add(volumeSlider);

            controlPanel.setMaximumSize(new Dimension(1080, 40));
        } catch (IOException e) {
            // IOException thrown while reading resource files
            e.printStackTrace();
        }
        return controlPanel;
    }

    /**
     * Creates shiTunes file menu
     *
     * @return the shiTunes file menu
     */
    private JMenu getFileMenu() {
        JMenu menu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem addItem = new JMenuItem("Add Song");
        JMenuItem deleteItem = new JMenuItem("Delete Song(s)");
        JMenuItem createPlaylistItem = new JMenuItem("Create Playlist");
        JMenuItem exitItem = new JMenuItem("Exit");

        addItem.addActionListener(new AddSongListener());
        deleteItem.addActionListener(new DeleteSongListener());
        openItem.addActionListener(new OpenItemListener());
        createPlaylistItem.addActionListener(new CreatePlaylistListener());
        exitItem.addActionListener(new ExitItemListener());

        menu.add(openItem);
        menu.add(addItem);
        menu.add(deleteItem);
        if(windowType == Window.MAIN) {
            menu.add(createPlaylistItem);
        }
        menu.add(exitItem);
        return menu;
    }

    /**
     * Creates shiTunes file menu
     *
     * @return the shiTunes file menu
     */
    private JMenu getControlsMenu() {
        JMenu menu = new JMenu("Controls");
        JMenuItem playItem = new JMenuItem("Play");
        JMenuItem nextItem = new JMenuItem("Next");
        JMenuItem previousItem = new JMenuItem("Previous");
        playRecentSubMenu = new JMenu("Play Recent");
        JMenuItem goToCurrentItem = new JMenuItem("Go To Current Song");
        JMenuItem increaseVolumeItem = new JMenuItem("Increase Volume");
        JMenuItem decreaseVolumeItem = new JMenuItem("Decrease Volume");
        shuffleItem = new JCheckBoxMenuItem("Shuffle");
        repeatItem = new JCheckBoxMenuItem("Repeat");

        // Build play recent menu
        updateRecentSongsMenu();

        // Set accelerators
        playItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        nextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.META_MASK));
        previousItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.META_MASK));
        goToCurrentItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.META_MASK));
        increaseVolumeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.META_MASK));
        decreaseVolumeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.META_MASK));

        // Add action listeners
        playItem.addActionListener(new PlayListener());
        nextItem.addActionListener(new NextListener());
        previousItem.addActionListener(new PreviousListener());
        goToCurrentItem.addActionListener(new GoToCurrentListener());
        increaseVolumeItem.addActionListener(new VolumeIncreaseListener());
        decreaseVolumeItem.addActionListener(new VolumeDecreaseListener());
        shuffleItem.addActionListener(new ShuffleListener());
        repeatItem.addActionListener(new RepeatListener());

        menu.add(playItem);
        menu.add(nextItem);
        menu.add(previousItem);
        menu.add(playRecentSubMenu);
        menu.add(goToCurrentItem);
        menu.addSeparator();
        menu.add(increaseVolumeItem);
        menu.add(decreaseVolumeItem);
        menu.addSeparator();
        menu.add(shuffleItem);
        menu.add(repeatItem);
        return menu;
    }

    private void updateRecentSongsMenu() {
        // Clear menu entries
        playRecentSubMenu.removeAll();

        // Repopulate with 10 most recent songs from db
        int[] recentSongs = ShiTunes.db.getRecentSongs();
        for(int i = 0; i < recentSongs.length; i++) {
            JMenuItem recentSongItem = new JMenuItem(ShiTunes.db.getSongTitle(recentSongs[i]));
            playRecentSubMenu.add(recentSongItem);
        }
    }

    /**
     * Initializes popup menu for the music table
     * <p>
     * When user right clicks anywhere on music table
     * a popup menu is displayed.
     *
     */
    private void createMusicTablePopupMenu() {
        musicTablePopupMenu = new JPopupMenu();
        JMenuItem addMenuItem = new JMenuItem("Add Song");
        JMenuItem deleteMenuItem = new JMenuItem("Delete Song(s)");
        addSongToPlaylistSubMenu = new JMenu("Add Song to Playlist");

        addMenuItem.addActionListener(new AddSongListener());
        deleteMenuItem.addActionListener(new DeleteSongListener());

        musicTablePopupMenu.add(addMenuItem);
        musicTablePopupMenu.add(deleteMenuItem);
        if(windowType == Window.MAIN) {
            musicTablePopupMenu.add(addSongToPlaylistSubMenu);
        }
        updateAddPlaylistSubMenu();
    }
    /**
     * Initializes a popup menu when a user right clicks for the playlist nodes.
     *
     * When the user right clicks on a tree node, the menu appears.
     */
    private void createPlaylistPopupMenu() {
        playlistPopupMenu = new JPopupMenu();
        JMenuItem deletePlaylist = new JMenuItem("Delete Playlist");
        JMenuItem newWindow = new JMenuItem("Open Playlist in New Window");
        deletePlaylist.addActionListener(new DeletePlaylistListener());
        newWindow.addActionListener(new NewWindowListener());
        playlistPopupMenu.add(deletePlaylist);
        playlistPopupMenu.add(newWindow);
    }

    /**
     * Initializes a popup menu when a user right clicks table column header.
     * Shows checkboxes to allow user to select which columns to display.
     */
    private void createShowColumnsPopupMenu() {
        showColumnsPopupMenu = new JPopupMenu();
        final JCheckBoxMenuItem showArtist = new JCheckBoxMenuItem("Artist");
        final JCheckBoxMenuItem showAlbum = new JCheckBoxMenuItem("Album");
        final JCheckBoxMenuItem showYear = new JCheckBoxMenuItem("Year");
        final JCheckBoxMenuItem showGenre = new JCheckBoxMenuItem("Genre");
        final JCheckBoxMenuItem showComment = new JCheckBoxMenuItem("Comment");

        // Set checkbox to reflect columns' visibility state (default is unselected)
        if (ShiTunes.db.getColumnVisible("Artist")) {showArtist.setSelected(true);}
        if (ShiTunes.db.getColumnVisible("Album")) {showAlbum.setSelected(true);}
        if (ShiTunes.db.getColumnVisible("Year")) {showYear.setSelected(true);}
        if (ShiTunes.db.getColumnVisible("Genre")) {showGenre.setSelected(true);}
        if (ShiTunes.db.getColumnVisible("Comment")) {showComment.setSelected(true);}

        showColumnsPopupMenu.add(showArtist);
        showColumnsPopupMenu.add(showAlbum);
        showColumnsPopupMenu.add(showYear);
        showColumnsPopupMenu.add(showGenre);
        showColumnsPopupMenu.add(showComment);

        ActionListener artistCheckboxListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (showArtist.isSelected()) { musicTable.show("Artist"); }
                else {musicTable.hide("Artist");}
            }
        };

        ActionListener albumCheckboxListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (showAlbum.isSelected()) { musicTable.show("Album"); }
                else { musicTable.hide("Album"); }
            }
        };

        ActionListener yearCheckboxListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (showYear.isSelected()) { musicTable.show("Year"); }
                else { musicTable.hide("Year"); }
            }
        };

        ActionListener genreCheckboxListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (showGenre.isSelected()) { musicTable.show("Genre"); }
                else { musicTable.hide("Genre"); }
            }
        };

        ActionListener commentCheckboxListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (showComment.isSelected()) { musicTable.show("Comment"); }
                else { musicTable.hide("Comment"); }
            }
        };

        showArtist.addActionListener(artistCheckboxListener);
        showAlbum.addActionListener(albumCheckboxListener);
        showYear.addActionListener(yearCheckboxListener);
        showGenre.addActionListener(genreCheckboxListener);
        showComment.addActionListener(commentCheckboxListener);
    }

    /**
     * Updates the playlist sub menu in the music table's popup menu:
     * <ul>
     *     <li>Gets an updated list of playlist names from database</li>
     *     <li>Removes all items from the playlist sub menu</li>
     *     <li>Repopulate the playlist sub menu</li>
     *     <li>Repaint the music table popup menu (in which the playlist sub menu resides</li>
     * </ul>
     *
     */
    private void updateAddPlaylistSubMenu() {
        // Get updated list of playlist names from database
        ArrayList<String> playlistNames = ShiTunes.db.getPlaylistNames();

        // Remove all items from music table popup menu - playlist sub menu
        addSongToPlaylistSubMenu.removeAll();

        // Repopulate music table popup menu - playlist sub menu
        for (String playlistName : playlistNames) {
            JMenuItem item = new JMenuItem(playlistName);
            item.addActionListener(new AddSongToPlaylistListener(playlistName));
            addSongToPlaylistSubMenu.add(item);
        }

        // Add terminating "Create Playlist" item to "Add Song to Playlist Menu"
        JMenuItem item = new JMenuItem("*Create Playlist*");
        item.addActionListener(new CreatePlaylistListener());
        addSongToPlaylistSubMenu.add(item);

        // Repaint the popup menu
        musicTablePopupMenu.repaint();

    }

    /* ******************** */
    /* Progress Bar Methods */
    /* ******************** */

    //Creates a JPanel for the progress bar and the two timers
    private JPanel getProgressBar()
    {
        JPanel progressBarPanel = new JPanel();
        leftTimer = new JLabel ("00:00:00");
        rightTimer = new JLabel ("00:00:00");
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("");

        //timer is used for the timer. Similar to threads, it runs in the background: actions happen based on time
        progressBarPanel.add(leftTimer);
        progressBarPanel.add(progressBar);
        progressBarPanel.add(rightTimer);

        return progressBarPanel;
    }

    //changes the jlabels timers for the progress bar
    private void updateProgress()
    {
        //used to format timer number
        NumberFormat format;
        format = NumberFormat.getNumberInstance();
        format.setMinimumIntegerDigits(2); //pad with 0 if necessary

        //right timer
        int rightHours = (int) (timeRemaining / (60000*60));
        int rightMinutes = (int) (timeRemaining / 60000);
        int rightSeconds = (int) ((timeRemaining % 60000) / 1000);

        //left timer
        int leftHours = (int) (timeElapsed / (60000*60));
        int leftMinutes = (int) (timeElapsed / 60000);
        int leftSeconds = (int) ((timeElapsed %60000)/1000);
        //set the left and right timer labels with new time

        //set the labels with format
        rightTimer.setText(format.format(rightHours) +":" + format.format(rightMinutes) + ":" + format.format(rightSeconds));
        leftTimer.setText(format.format(leftHours) +":" + format.format(leftMinutes) + ":" + format.format(leftSeconds));

        if(duration > 0) {
            // set progress bar value based on percentage of timeElapsed
            progressBar.setValue((int) ( ( (float) timeElapsed / duration) * 100));
        }
    }

    //used when changing songs
    private void clearProgressBar()
    {
        timeRemaining = 0;
        timeElapsed = 0;
        updateProgress();
        rightTimer.setText("00:00:00");
        leftTimer.setText("00:00:00");
        progressBar.setValue(0);
    }

    /* ********************* */
    /* Music Table Listeners */
    /* ********************* */

    /**
     * Double Click Listener:
     * <p>
     * Plays the song that is double clicked.
     */
    private class DoubleClickListener extends MouseAdapter{
        public void mousePressed(MouseEvent me) {
            if (me.getClickCount() == 2) {
                int row = musicTable.getTable().getSelectedRow();
                playSong(row);
            }
        }
    }

    /**
     * Popup listener for the right click menu on Music Table
     *
     */
    private class MusicTablePopupListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                musicTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /**
     * Popup listener for the right click menu on Column Titles
     *
     */
    private class ColumnDisplayPopupListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }
        public void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showColumnsPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /**
     * Drop target listener on music table panel
     * when song(s) are dragged from the computer's
     * file browser into the music table they are added to the
     * table
     *
     * Conditions:
     * <ul>
     * <li>If this is the Library table:  add songs to table & database</li>
     * <li>If this is a Playlist table: add songs to table & playlist (& database if not already)</li>
     * </ul>
     */
    private class AddToTableDropTarget extends DropTarget {
        @Override
        public synchronized void drop(DropTargetDropEvent dtde) {
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            Transferable t = dtde.getTransferable();
            java.util.List fileList;
            try {
                fileList = (java.util.List) t.getTransferData(DataFlavor.javaFileListFlavor);
                for(Object file : fileList) {
                    Song song = new Song(file.toString());

                    if(musicTable.getType() == MusicTable.LIBRARY) {
                        // If this is the main application window & the music table == library
                        // Only add song to library table if it is not already present in db
                        int id = ShiTunes.db.insertSong(song);
                        if (id != -1) {
                            // if song successfully added to database
                            // add song to music library table
                            musicTable.addSongToTable(id, song);
                        }
                    } else if(musicTable.getType() == MusicTable.PLAYLIST) {

                        // If the music table == playlist
                        // Try to add song to db (if already in db it won't be added)
                        int id = ShiTunes.db.insertSong(song);

                        // Add song to the playlist
                        ShiTunes.db.addSongToPlaylist(song.getFilePath(), selectedPlaylist);

                        // Get song id if the song was already in library
                        // (ie. id in previous assignment == -1)
                        if(id == -1) {
                            id = ShiTunes.db.getSongId(song.getFilePath());
                        }

                        // Add song to playlist table
                        musicTable.addSongToTable(id, song);

                        // Notify main application window table of change
                        // if this is a separate playlist window
                        if(windowType == Window.PLAYLIST) {
                            ShiTunes.mainWindow.musicTable.updateTableModel("Library");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* ************************ */
    /* Playlist Panel Listeners */
    /* ************************ */

    private class NewWindowListener implements  ActionListener {
        public void actionPerformed(ActionEvent e) {
            // Switch back to Library table in MAIN app window
            musicTable.updateTableModel("Library");

            // Set highlighted node in playlist panel to "Library"
            playlistPanelTree.setSelectionRow(0);

            // Open new window for selected playlist
            Window newWindow = new Window(selectedPlaylist);
            newWindow.display();
        }
    }

        /**
         * Mouse listener to handle left and right clicks within
         * the Playlist Panel
         *
         */
    private class PlaylistPanelMouseListener extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            JTree tree = (JTree) e.getSource();
            String selection = tree.getSelectionPath().getLastPathComponent().toString();

            if(selection.equals(null)) {
                selection = "Library";  // set selection to "Library" if null, as default
            }

            if(SwingUtilities.isRightMouseButton(e) && !selection.equals("Library")
                    && !selection.equals("Playlists")) {
                // An individual playlist was right clicked,

                // Set selected playlist
                selectedPlaylist = selection;

                // show popup menu
                maybeShowPopup(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            JTree tree = (JTree) e.getSource();
            String selection = tree.getSelectionPath().getLastPathComponent().toString();

            if(selection.equals(null)) {
                selection = "Library";  // set selection to "Library" if null, as default
            }

            // highlight selected row
            int row = tree.getClosestRowForLocation(e.getX(), e.getY());
            tree.setSelectionRow(row);

            if(SwingUtilities.isLeftMouseButton(e)) {
                // left click pressed

                // If selection is not Playlist
                // ie. "Library" or a playlist name was selected
                if(!selection.equals("Playlists")) {
                    // Update the table model
                    musicTable.updateTableModel(selection);

                    // If library selected: ensure add song to playlist sub menu gets added back
                    // Else if individual playlist selected: remove the add song to playlist sub menu, set selectedPlaylist
                    if(selection.equals("Library")) {
                        musicTablePopupMenu.add(addSongToPlaylistSubMenu);
                    } else {
                        selectedPlaylist = selection;
                        musicTablePopupMenu.remove(addSongToPlaylistSubMenu);
                    }

                    // Repaint the music table scroll pane
                    musicTableScrollPane.repaint();
                }
            }
        }

        public void maybeShowPopup(MouseEvent e) {
            playlistPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Listener that creates a new Playlist
     * <p>
     * When 'Create Playlist' is selected from main menu,
     * a popup appears to allow user to name their playlist.
     * New, empty playlist is added to database.
     * Window is refreshed to reflect changes.
     */
    private class CreatePlaylistListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            // Display message box with a textfield for user to type into
            JFrame createPLFrame = new JFrame("Create New Playlist");
            String playlistName = (String) JOptionPane.showInputDialog(createPLFrame, "New playlist's name: ",
                    "Create New Playlist", JOptionPane.PLAIN_MESSAGE);
            ShiTunes.db.addPlaylist(playlistName);

            // Refresh GUI popupmenu playlist sub menu
            updateAddPlaylistSubMenu();
            updatePlaylistNode();
            ((DefaultTreeModel)playlistPanelTree.getModel()).reload();

            // Expand playlist node (index 1)
            playlistPanelTree.expandRow(1);

            // Select playlist node just created
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) playlistPanelTree.getModel().getRoot();
            DefaultMutableTreeNode playlistsNode = (DefaultMutableTreeNode) playlistPanelTree.getModel().getChild(rootNode, 1);
            TreePath path = new TreePath(rootNode);
            path = path.pathByAddingChild(playlistsNode);
            int numPlaylists = playlistPanelTree.getModel().getChildCount(playlistsNode);
            for(int i = 0; i < numPlaylists; i++) {
                String node = playlistPanelTree.getModel().getChild(playlistsNode, i).toString();
                if(node.equals(playlistName)) {
                    path = path.pathByAddingChild(playlistsNode.getChildAt(i));
                    playlistPanelTree.addSelectionPath(path);
                }
            }

            // Update selected playlist
            selectedPlaylist = playlistName;

            // Update table model
            musicTable.updateTableModel(playlistName);
        }
    }

    /**
     * Listener that deletes an existing Playlist
     * <p>
     * When 'Delete Playlist' is selected from highlighted side panel,
     * a popup appears to confirm deletion.
     * Window is refreshed to reflect changes.
     *
     */
    private class DeletePlaylistListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            JFrame confirmDeleteFrame = new JFrame("Delete Playlist");
            int answer = JOptionPane.showConfirmDialog(confirmDeleteFrame,
                    "Are you sure you want to delete this playlist?");
            if (answer == JOptionPane.YES_OPTION) {
                // Delete selected playlist from library
                ShiTunes.db.deletePlaylist(selectedPlaylist);

                // Refresh playlist panel tree
                updatePlaylistNode();

                // may need to add tree redraw or something
                ((DefaultTreeModel)playlistPanelTree.getModel()).reload(playlistNode);

                // Refresh GUI popupmenu playlist sub menu
                updateAddPlaylistSubMenu();

            }
        }
    }

    /**
     * Add song(s) to playlist listener.
     * <p>
     * Adds the selected song(s) to the given playlist
     * Note: the playlist name must be passed as a parameter
     *
     */
    private class AddSongToPlaylistListener implements ActionListener {
        private String playlist;

        public AddSongToPlaylistListener(String playlistName) {
            playlist = playlistName;
        }

        public void actionPerformed(ActionEvent event) {
            int[] selectedRows = musicTable.getTable().getSelectedRows();

            for(int i = 0; i < selectedRows.length; i++) {
                String selectedSong = musicTable.getTable().getValueAt(
                        selectedRows[i], MusicTable.COL_FILE_PATH).toString();
                ShiTunes.db.addSongToPlaylist(selectedSong, playlist);
            }
            // Expand playlist node (index 1)
            playlistPanelTree.expandRow(1);
        }
    }

    /* ***************** */
    /* Control Listeners */
    /* ***************** */

    /**
     * A listener for the Previous Song action
     */
    private class PreviousListener implements ActionListener {
        /**
         * If player state is currently playing/resumed
         * stop the current song, decrement the song index
         * and play the previous song
         *
         * @param e the ActionEvent object for this event
         */
        public void actionPerformed(ActionEvent e) {
            int previousSongRow = player.getLoadedSongRow() - 1;

            // Only skip to previous if the loaded song is not the first item in the table
            // and the loaded song is not set to -1 flag (which indicates that the
            // loaded song was opened via the File->Open menu)
            if(previousSongRow >= 0 || shuffleItem.isSelected()) {
                if(playerState == BasicPlayerEvent.PLAYING ||
                   playerState == BasicPlayerEvent.RESUMED) {
                    // if player is currently playing/resumed
                    // stop current song
                    // decrement player.currentSongIndex
                    // play previous song
                    player.stop();

                    playSong(previousSongRow);
                }
            }
        }
    }

    /**
     * Play Listener:
     * <p>
     * Plays the selected song loaded, if the conditions are right.
     *
     */
    private class PlayListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int selectedRow = musicTable.getTable().getSelectedRow();
            // boolean indicator, true if selected song is currently loaded to player
            boolean selectedSongIsLoaded =
                     selectedRow == player.getLoadedSongRow();

            if (selectedSongIsLoaded && playerState == BasicPlayerEvent.PAUSED) {
                // if selected song is current song on player
                // and player.state == paused
                player.resume();
            } else {
                if(selectedRow == -1) {
                    // if no row selected:
                    // set loaded song to first song in table
                    selectedRow = 0;
                }
                if (playerState == BasicPlayerEvent.PLAYING ||
                    playerState == BasicPlayerEvent.RESUMED ||
                    playerState == BasicPlayerEvent.PAUSED) {
                    // stop player
                    player.stop();
                }
                playSong(selectedRow);
            }
        }
    }

    /**
     * Pause Listener:
     * <p>
     * Pauses the currently playing song
     *
     */
    private class PauseListener implements ActionListener {
        /**
         * Calls the MusicPlayer pause function when event occurs
         *
         * @param e the ActionEvent object for this event
         */
        public void actionPerformed(ActionEvent e) {
            if(playerState == BasicPlayerEvent.PLAYING ||
               playerState == BasicPlayerEvent.RESUMED) {
                player.pause();
            }
        }
    }

    /**
     * Stops Listener:
     * <p>
     * Stops currently playing song
     *
     */
    private class StopListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            player.stop();
            clearProgressBar();
        }
    }

    /**
     * Next Listener:
     * <p>
     * Highlights and plays the next song in the table (if there is one)
     *
     */
    private class NextListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int nextSongIndex = player.getLoadedSongRow() + 1;
            int lastItemInTable = musicTable.getTable().getRowCount() - 1;

            // Only skip to next if the loaded song is not the last item in the table
            // or shuffle is selected
            if(nextSongIndex <= lastItemInTable || shuffleItem.isSelected()) {
                if(playerState == BasicPlayerEvent.PLAYING ||
                   playerState == BasicPlayerEvent.RESUMED) {
                    player.stop();  // stop currently playing song
                }
                playSong(nextSongIndex);
            }
        }
    }

    /**
     * Volume slider listener:
     * <p>
     * Takes value from volume slider
     * and converts to double in range [0.0, 1.0] to
     * set basic player gain (volume) to a value it understands.
     *
     */
    private class VolumeSliderListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                // slider value in range [0, 100]
                // converted to double value in range [0.0, 1.0]
                // which is the range required by BasicPlayer setGain() method
                double volume = source.getValue() / 100.00;
                player.adjustVolume(volume);
            }
        }
    }

    /* ********************** */
    /* Control Menu Listeners */
    /* ********************** */

    /**
     * Volume increment listener:
     * <p>
     * Increases volume by 5%
     *
     */
    private class VolumeIncreaseListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            double volume = player.getVolume();
            if(volume < .95) {
                // Increase by .05 (which is 5% of total gain value)
                player.adjustVolume(volume + .05);
            } else {
                // if volume > .95, increase volume to 100
                // (otherwise [volume + .05] in if statement would have increased gain above 1.0)
                player.adjustVolume(1.0);
            }
            // Adjust volume slider
            volumeSlider.setValue(player.getSliderVolume());
        }
    }

    /**
     * Volume decrement listener:
     * <p>
     * Decreases volume by 5%
     *
     */
    private class VolumeDecreaseListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            double volume = player.getVolume();
            if(volume > 0.05) {
                // Decrease by .05 (which is 5% of total gain value)
                player.adjustVolume(volume - .05);
            } else {
                // if volume < .05, reduce volume to zero
                // (otherwise [volume - .05] in if statement would have reduced gain below zero)
                player.adjustVolume(0);
            }
            // Adjust volume slider
            volumeSlider.setValue(player.getSliderVolume());
        }
    }

    /**
     * Scroll to currently loaded song row listener
     *
     */
    private class GoToCurrentListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!(musicTable.getTable().getParent() instanceof JViewport)) {
                return;
            }

            // Get cell rectangle for loaded song row
            Rectangle rect = musicTable.getTable().getCellRect(player.getLoadedSongRow(), 0, true);

            musicTable.getTable().scrollRectToVisible(rect);
        }
    }

    /**
     * Shuffle listener
     *
     * If shuffle has been checked: repeat is disabled
     * If shuffle has been unchecked: repeat is enabled
     *
     * If shuffle has been checked && player is not playing:
     *      playSong() is called which will pay a random song since shuffle is enabled
     *
     */
    private class ShuffleListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if(shuffleItem.isSelected()){
                repeatItem.setEnabled(false);
                if(playerState != BasicPlayerEvent.PLAYING) {
                    // player was not playing - thus, play random song
                    // (value passed to playSong does not matter as a random song will be selected)
                    playSong(0);
                }
            } else {
                repeatItem.setEnabled(true);
            }
        }
    }

    /**
     * Repeat listener
     *
     * If repeat has been checked: shuffle is disabled
     * If repeat has been unchecked: shuffle is enabled
     *
     */
    private class RepeatListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if(repeatItem.isSelected()){
                shuffleItem.setEnabled(false);
            } else {
                shuffleItem.setEnabled(true);
            }
        }
    }

    /* ******************* */
    /* File Menu Listeners */
    /* ******************* */

    /**
     * Open Item listener:
     * <p>
     * Opens and plays the selected song using
     * "quickPlay" method, which
     *
     */
    private class OpenItemListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("MP3 Files", "mp3");
                chooser.setFileFilter(filter);  //filters for mp3 files only
                //file chooser menu
                if (chooser.showDialog(windowFrame, "Open Song") == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = chooser.getSelectedFile();
                    Song selectedSong = new Song(selectedFile.getPath());
                    if (playerState == BasicPlayerEvent.PLAYING ||
                        playerState == BasicPlayerEvent.RESUMED ||
                        playerState == BasicPlayerEvent.PAUSED) {
                        // player.state == playing/resumed/paused
                        // stop player
                        player.stop();
                    }

                    player.play(selectedSong.getFilePath());
                }
            }
    }

    /**
     * Add Song Listener:
     * <p>
     * Opens a file chooser allowing user to select a song file
     * to add to the library/playlist.  The selected song is then added
     * to the library/playlist.
     * <p>
     * Then all application Windows tables are updated in the
     * event that the song(s) being removed from the table is
     * also present in another window/table.
     *
     */
    private class AddSongListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("MP3 Files", "mp3");
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(filter);  //filters for mp3 files only
            //file chooser menu
            if (chooser.showDialog(windowFrame, "Add Song") == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                Song selectedSong = new Song(selectedFile.getPath());
                int id = ShiTunes.db.insertSong(selectedSong);  // -1 if failure

                if(musicTable.getType() == MusicTable.LIBRARY) {
                    // If the music table == library
                    // Only add song to library table if it is not already present in db
                    if (id != -1) {
                        // if song successfully added to database
                        // add song to music library table
                        musicTable.addSongToTable(id, selectedSong);
                    }
                } else if(musicTable.getType() == MusicTable.PLAYLIST){
                    // If the music table == playlist
                    // Add song to the playlist
                    ShiTunes.db.addSongToPlaylist(selectedSong.getFilePath(), selectedPlaylist);
                    // Add song to playlist table
                    musicTable.addSongToTable(id, selectedSong);
                }

                ShiTunes.updateAllWindows();
            }
        }
    }

    /**
     * Delete Song Listener:
     * <p>
     * Deletes the selected range of songs from the table &
     * the database (if MusicTable.LIBRARY) or playlist
     * (if MusicTable.PLAYLIST).
     * <p>
     * Then all application Windows tables are updated in the
     * event that the song(s) being removed from the table is
     * also present in another window/table.
     *
     */
    private class DeleteSongListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            int[] selectedRows = musicTable.getTable().getSelectedRows();

            DefaultTableModel model = (DefaultTableModel) musicTable.getTable().getModel();

            /*
            * Cycle through all selected songs and delete
            * one at a time
            *
            */
            for(int i = 0; i < selectedRows.length; i++) {
                int selectedSongRow = selectedRows[i];
                int selectedSongId = Integer.parseInt(musicTable.getTable().getValueAt(
                        selectedSongRow, MusicTable.COL_ID).toString());

                // Stop player if song being deleted is the current song on the player
                // and clear progress bar
                if(selectedSongRow == player.getLoadedSongRow()) {
                    player.stop();
                    clearProgressBar();
                }

                model.removeRow(selectedSongRow);

                if(musicTable.getType() == MusicTable.LIBRARY) {
                    // Delete song from database by using filepath as an identifier
                    ShiTunes.db.deleteSong(selectedSongId);
                } else if(musicTable.getType() == MusicTable.PLAYLIST){
                    ShiTunes.db.deleteSongFromPlaylist(selectedSongId, selectedPlaylist);
                }
            }

            // Update all windows in the event that the song(s) being removed from the table
            // is also present in another window/table
            ShiTunes.updateAllWindows();

            // Update recent songs menu (in case any recent songs were deleted)
            updateRecentSongsMenu();
        }
    }

    /**
     * Exit item listener:
     * <p>
     * Closes the database connection and
     * exit the shiTunes program gracefully
     *
     */
    private class ExitItemListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if(windowType == Window.MAIN) {
                ShiTunes.db.close();
                System.exit(0);
            } else if(windowType == Window.PLAYLIST) {
                windowFrame.dispatchEvent(new WindowEvent(windowFrame, WindowEvent.WINDOW_CLOSING));
            }
        }
    }

    /* *********************** *
     * Player Callback Methods *
     * *********************** */

    /**
     * Open callback, stream is ready to play.
     *
     * properties map includes audio format dependant features such as
     * bitrate, duration, frequency, channels, number of frames, vbr flag, ...
     *
     * @param stream could be File, URL or InputStream
     * @param properties audio stream properties.
     */
    public void opened(Object stream, Map properties)
    {
        duration = Integer.parseInt(properties.get("duration").toString()) / 1000;
    }

    /**
     * Progress callback while playing.
     *
     * This method is called several time per seconds while playing.
     * properties map includes audio format features such as
     * instant bitrate, microseconds position, current frame number, ...
     *
     * @param bytesread from encoded stream.
     * @param microseconds elapsed (<b>reseted after a seek !</b>).
     * @param pcmdata PCM samples.
     * @param properties audio stream parameters.
     */
    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties)
    {
        timeElapsed = microseconds/1000;
        timeRemaining = duration - timeElapsed;
        updateProgress();

        // if time remaining less than 1 second, set songCompleted flag to true
        if(timeRemaining < 1000) {
            songCompleted = true;
            clearProgressBar();
        }
    }

    /**
     * Notification callback for basicplayer events such as opened
     * <p>
     * States Codes - see state variable comment
     *
     * @param event the basicplayer event (OPENED, PAUSED, PLAYING, SEEKING...)
     */
    public void stateUpdated(BasicPlayerEvent event)
    {
        // Notification of BasicPlayer states (opened, playing, end of media, ...)
        if(event.getCode() != BasicPlayerEvent.GAIN) {
            // if state is not GAIN (due to volume change)
            // update state code
            playerState = event.getCode();
        } else {
            // do nothing, retain previous state
        }

        if(playerState == BasicPlayerEvent.STOPPED && songCompleted) {
            NextListener nextListener = new NextListener();
            nextListener.actionPerformed(null);
            songCompleted = false;
        }
    }

    /**
     * Public accessor for player state
     * <p>
     * States Codes - see state variable comment
     *
     * @return the players state
     */
    public int getState() {
        return playerState;
    }

    /**
     * A handle to the BasicPlayer, plugins may control the player through
     * the controller (play, stop, ...)
     *
     * @param controller a handle to the player
     */
    public void setController(BasicController controller)
    {
        // System.out.println("setController : " + controller);
    }

    /* ********************* *
    * Window related methods *
    * ********************** */

     /**
     * Window listener for Window.type == PLAYLIST
     *
     */
    private class PlaylistWindowListener implements WindowListener {
         @Override
         public void windowActivated(WindowEvent e) {
         }

         @Override
         public void windowClosed(WindowEvent e) {
             // Remove window from list of application windows
             ShiTunes.windows.remove(this);
         }

         @Override
         public void windowClosing(WindowEvent e) {
         }

         @Override
         public void windowDeactivated(WindowEvent e) {
         }

         @Override
         public void windowDeiconified(WindowEvent e) {
         }

         @Override
         public void windowOpened(WindowEvent e) {
         }

         @Override
         public void windowIconified(WindowEvent e) {
         }
    }

    /*
     * Plays given song row and handles other Window related
     * updates that should take place (progress bar, row highlighting)
     *
     * @param row
     */
    private void playSong(int row) {
        // If shuffle mode on, switch to random row
        if (shuffleItem.isSelected()) {
            Random r = new Random();
            row = r.nextInt(musicTable.getTable().getRowCount());
        } else if(repeatItem.isSelected()) {
            row = player.getLoadedSongRow();
        }

        clearProgressBar();
        int songId = Integer.parseInt(musicTable.getTable().getValueAt(row, MusicTable.COL_ID).toString());
        player.setLoadedSongRow(row);
        musicTable.getTable().setRowSelectionInterval(row, row);
        player.play(ShiTunes.db.getSongFilePath(songId));
        ShiTunes.db.addRecentSong(songId);
        updateRecentSongsMenu();

        // scroll to song being played
        GoToCurrentListener goToCurrentSong = new GoToCurrentListener();
        goToCurrentSong.actionPerformed(null);
    }
}