package simulationcontroller;

import java.util.ArrayList;
import java.util.Comparator;

class Queue {

    private static final class Node {

        Process val;
        Node next;

        Node(Process v) {
            this.val = v;
        }
    }

    private Node head, tail;

    private int count = 0;

    public void enqueue(Process p) {

        Node n = new Node(p);

        if (tail == null) {

            head = tail = n;

        } else {

            tail.next = n;

            tail = n;

        }

        count++;

    }

    public Process dequeue() {

        if (head == null) {
            return null;
        }

        Process v = head.val;

        head = head.next;

        if (head == null) {
            tail = null;
        }

        count--;

        return v;

    }

    public Process peek() {
        if (head == null) {
            return null;
        }
        return head.val;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public ArrayList<Process> getAll() {
        ArrayList<Process> list = new ArrayList<>();
        for (Node n = head; n != null; n = n.next) {
            list.add(n.val);
        }

        return list;
    }

    public void sort(String mode) {
        if (count <= 1) {
            return;
        }

        ArrayList<Process> list = getAll();

        switch (mode.toLowerCase()) {
            case "mem":
                list.sort(new Comparator<Process>() {
                    @Override
                    public int compare(Process p1, Process p2) {
                        return Long.compare(p1.getMemoryRequired(), p2.getMemoryRequired());
                    }
                });
                break;

            case "rrtag":
                list.sort(new Comparator<Process>() {
                    @Override
                    public int compare(Process p1, Process p2) {
                        return Long.compare(p1.getRrTag(), p2.getRrTag());
                    }
                });
                break;

            default:
                list.sort(new Comparator<Process>() {
                    @Override
                    public int compare(Process p1, Process p2) {
                        return Long.compare(p1.getProcessId(), p2.getProcessId());
                    }
                });
                break;
        }

        // Rebuild the queue
        head = tail = null;
        count = 0;

        for (Process p : list) {
            enqueue(p);
        }
    }

    public int size() {
        return count;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "EMPTY";
        }

        StringBuilder sb = new StringBuilder();
        for (Process p : getAll()) {
            sb.append(String.format("Job ID %d (Rem=%d) \n",
                    p.getProcessId(),
                    p.getRemainingBurst()));
        }
        return sb.toString();
    }
}
