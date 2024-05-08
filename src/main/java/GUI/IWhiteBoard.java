package GUI;

import WBSYS.*;
import io.grpc.ManagedChannel;

import java.util.ArrayList;

public interface IWhiteBoard{
    IClient getSelfUI();
    void SynchronizeCanvas(CanvasShape canvasShape);

    void SynchronizeMessage(String chatMessage);

    void SynchronizeUser(String operation, String username);

    boolean getApproveFromUI(String request);

    void registerPeer(String username, String IpAddress, String port, ManagedChannel channel);

    void registerManager(String IpAddress, String port, String name, ManagedChannel channel);

    ArrayList<CanvasShape> getCanvasShapeArrayList();

    void reportUpdEditing(String operation, String username);

    void removePeer(String username);

    void peerExit(String username);

    void newFile();

    void openFile(ArrayList<CanvasShape> newShapes);

    void managerClose();

    void broadCastEditing(String operation, String username);

    void updEditing(String operation, String username);
}