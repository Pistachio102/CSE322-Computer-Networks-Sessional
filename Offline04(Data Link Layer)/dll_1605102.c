#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ******************************************************************
 ALTERNATING BIT AND GO-BACK-N NETWORK EMULATOR: SLIGHTLY MODIFIED
 FROM VERSION 1.1 of J.F.Kurose

   This code should be used for PA2, unidirectional or bidirectional
   data transfer protocols (from A to B. Bidirectional transfer of data
   is for extra credit and is not required).  Network properties:
   - one way network delay averages five time units (longer if there
       are other packets in the channel for GBN), but can be larger
   - packets can be corrupted (either the header or the data portion)
       or lost, according to user-defined probabilities
   - packets will be delivered in the order in which they were sent
       (although some can be lost).
**********************************************************************/
#define payload_size 4
#define BIDIRECTIONAL 1 /* change to 1 if you're doing extra credit */
/* and write a routine called B_output */

/* a "msg" is the data unit passed from layer 5 (teachers code) to layer  */
/* 4 (students' code).  It contains the data (characters) to be delivered */
/* to layer 5 via the students transport level protocol entities.         */
struct pkt
{
    char data[payload_size];
};

/* a packet is the data unit passed from layer 4 (students code) to layer */
/* 3 (teachers code).  Note the pre-defined packet structure, which all   */
/* students must follow. */
struct frm
{
    int typeOfFrame;
    int seqnum;
    int acknum;
    int checksum;
    char payload[payload_size];
};

/********* FUNCTION PROTOTYPES. DEFINED IN THE LATER PART******************/
void starttimer(int AorB, float increment);
void stoptimer(int AorB);
void tolayer1(int AorB, struct frm frame);
void tolayer3(int AorB, char datasent[payload_size]);

/********* ASSISTIVE GLOBAL VARIABLES AND FUNCTIONS FOR THE CODE *********/

#define timer_length 50
#define A 0
#define B 1

int data_frame_no_A = -1;            // starts from 1
int data_frame_no_B = -1;
int acknowledgement_frame_no_A = -1; // starts from 1
int acknowledgement_frame_no_B = -1;
int outstandingACK_A = -1;
int outstandingACK_B = -1;
int is_medium_busy = -1;
int piggybacked = -1;           // 0 for not busy, 1 for busy
struct frm stored_frame_A;
struct frm stored_frame_B;

int calculate_checksum(struct frm temp_frame)
{
    int sum  = 0;

    sum += temp_frame.typeOfFrame;
    sum += temp_frame.seqnum;
    sum += temp_frame.acknum;

    for (int i = 0; i < payload_size; ++i)
    {
        sum += (int) temp_frame.payload[i];
    }

    return sum;
}

struct frm make_data_frame(int frame_no, struct pkt packet)
{
    struct frm data_frame;
    data_frame.typeOfFrame = 0;
    data_frame.seqnum = frame_no;
    data_frame.acknum = -1;
    memcpy(data_frame.payload, packet.data, sizeof(packet.data));
    data_frame.checksum = calculate_checksum(data_frame);
    return data_frame;
}

struct frm make_acknowledgement_frame(int frame_no)
{
    struct frm acknowledgement_frame;
    acknowledgement_frame.typeOfFrame = 1;
    acknowledgement_frame.seqnum = -1;
    acknowledgement_frame.acknum = frame_no;
    memset(acknowledgement_frame.payload, 0, sizeof(acknowledgement_frame.payload));
    acknowledgement_frame.checksum = calculate_checksum(acknowledgement_frame);
    return acknowledgement_frame;
}

struct frm make_piggybacked_acknowledgement_frame(int data_frame_no, struct pkt packet, int acknowledgement_frame_no)
{
    struct frm acknowledgement_frame;
    acknowledgement_frame.typeOfFrame = 2;
    acknowledgement_frame.seqnum = data_frame_no;
    acknowledgement_frame.acknum = acknowledgement_frame_no;
    memcpy(acknowledgement_frame.payload, packet.data, sizeof(packet.data));
    acknowledgement_frame.checksum = calculate_checksum(acknowledgement_frame);
    return acknowledgement_frame;
}

/********* STUDENTS WRITE THE NEXT SEVEN ROUTINES *********/

/* called from layer 5, passed the data to be sent to other side */
void A_output(struct pkt packet)
{
    if (is_medium_busy != 0)
    {
        printf("  A_output: Medium is currently occupied with another frame. Dropping the frame.\n");
        return;
    }

    is_medium_busy = 1;

    struct frm data_frame;

    if (piggybacked == 1 && outstandingACK_A == 1)
    {
        data_frame = make_piggybacked_acknowledgement_frame(data_frame_no_A, packet, acknowledgement_frame_no_A);
        outstandingACK_A = 0;
        printf("  A_output: Sending piggybacked frame with msg: %s with checksum: %d and acknowledgement: %d to medium.\n", data_frame.payload, data_frame.checksum, data_frame.acknum);

    }
    else
    {
        data_frame = make_data_frame(data_frame_no_A, packet);
        printf("  A_output: Sending frame with msg: %s with checksum: %d to medium.\n", data_frame.payload, data_frame.checksum);
    }

    tolayer1(A, data_frame);
    printf("  A_output: Storing the frame and starting the timer with time units: %d.\n", timer_length);
    stored_frame_A = data_frame;
    starttimer(A, timer_length);
}

/* need be completed only for extra credit */
void B_output(struct pkt packet)
{
    if (is_medium_busy != 0)
    {
        printf("  B_output: Medium is currently occupied with another frame. Dropping the frame.\n");
        return;
    }

    is_medium_busy = 1;


    struct frm data_frame;

    if (piggybacked == 1 && outstandingACK_B == 1)
    {
        data_frame = make_piggybacked_acknowledgement_frame(data_frame_no_B, packet, acknowledgement_frame_no_B);
        outstandingACK_B = 0;
        printf("  B_output: Sending piggybacked frame with msg: %s with checksum: %d and acknowledgement: %d to medium.\n", data_frame.payload, data_frame.checksum, data_frame.acknum);

    }
    else
    {
        data_frame = make_data_frame(data_frame_no_B, packet);
        printf("  B_output: Sending frame with msg: %s with checksum: %d to medium.\n", data_frame.payload, data_frame.checksum);
    }

    tolayer1(B, data_frame);
    printf("  B_output: Storing the frame and starting the timer with time units: %d.\n", timer_length);
    stored_frame_B = data_frame;
    starttimer(B, timer_length);
}

/* called from layer 3, when a packet arrives for layer 4 */
void A_input(struct frm frame)
{
    printf("  A_input: Received new frame in A. Updating medium status.\n");
    is_medium_busy = 0;
    if (frame.typeOfFrame == 0)
    {
        if (frame.checksum != calculate_checksum(frame))
        {
            printf("  A_input: Received corrupt frame: %d with msg: %s and checksum: %d and calculated checksum: %d from medium. Resending the previously received frame acknowledgement.\n", frame.seqnum, frame.payload, frame.checksum, calculate_checksum(frame));
            struct frm acknowledgement_frame = make_acknowledgement_frame(acknowledgement_frame_no_A);
            tolayer1(A, acknowledgement_frame);
            return;
        }
        else if (frame.seqnum != acknowledgement_frame_no_A + 1)
        {
            printf("  A_input: Received duplicate frame: %d with msg: %s and checksum: %d from medium. Resending the previously received frame acknowledgement.\n", frame.seqnum, frame.payload, frame.checksum);
            if (piggybacked == 1)
            {
                outstandingACK_A = 0;
            }
            struct frm acknowledgement_frame = make_acknowledgement_frame(acknowledgement_frame_no_A);
            tolayer1(A, acknowledgement_frame);
            return;
        }
        else {
            printf("  A_input: Received valid frame: %d with msg: %s and checksum: %d from medium. ", frame.seqnum, frame.payload, frame.checksum);

            ++acknowledgement_frame_no_A;
        }
        if (piggybacked == 1)
        {
            printf("Awaiting piggybacked frame acknowledgement.\n");
            outstandingACK_A = 1;
        } 
        else 
        {
            printf("Sending new frame acknowledgement.\n");
            struct frm acknowledgement_frame = make_acknowledgement_frame(acknowledgement_frame_no_A);
            tolayer1(A, acknowledgement_frame);
        }
    }

    else if (frame.typeOfFrame == 1)
    {
        if (frame.checksum != calculate_checksum(frame))
        {
            printf("  A_input: Received corrupt acknowledgement of frame: %d with checksum: %d and calculated checksum: %d from medium. Ignoring the frame.\n", frame.acknum, frame.checksum, calculate_checksum(frame));
            return;
        }
        else if (frame.acknum != data_frame_no_A)
        {
            printf("  A_input: Received duplicate acknowledgement of frame: %d from medium. Ignoring the frame.\n", frame.acknum);
            return;
        }
        else {
            printf("  A_input: Received acknowledgement of frame: %d from medium. Updating the frame no. and medium status.\n", frame.acknum);

            ++data_frame_no_A;

            printf("  A_input: Stopping the timer.\n");
            stoptimer(A);

        }
    }

    else if (frame.typeOfFrame == 2)
    {
        if (frame.checksum != calculate_checksum(frame))
        {
            printf("  A_input: Received corrupt frame: %d with msg: %s and checksum: %d and calculated checksum: %d from medium. Ignoring the frame.\n", frame.seqnum, frame.payload, frame.checksum, calculate_checksum(frame));
            return;
            
        }
        if (frame.seqnum != acknowledgement_frame_no_A + 1)
        {
            printf("  A_input: Received duplicate data from frame: %d with msg: %s and checksum: %d from medium. Sending previous acknowledgement.\n", frame.seqnum, frame.payload, frame.checksum);
            
            if (frame.acknum != data_frame_no_A)
            {
                printf("  A_input: Received duplicate acknowledgement of frame: %d from medium. Ignoring the acknowledgement.\n", frame.acknum);
            }
            else {
                printf("  A_input: Received valid acknowledgement of frame: %d from medium.\n", frame.acknum);

                ++data_frame_no_A;

                printf("  A_input: Stopping the timer.\n");
                stoptimer(A);

            }

            outstandingACK_A = 0;
            struct frm acknowledgement_frame = make_acknowledgement_frame(acknowledgement_frame_no_A);
            tolayer1(A, acknowledgement_frame);

        }
        else
        {
            printf("  A_input: Received valid frame: %d with msg: %s and checksum: %d from medium. Awaiting new data at A.\n", frame.seqnum, frame.payload, frame.checksum);

            ++acknowledgement_frame_no_A;
            outstandingACK_A = 1;
            if (frame.acknum != data_frame_no_A)
            {
                printf("  A_input: Received duplicate acknowledgement of frame: %d from medium. Ignoring the frame.\n", frame.acknum);
            }
            else {
                printf("  A_input: Received valid acknowledgement of frame: %d from medium.\n", frame.acknum);

                ++data_frame_no_A;

                printf("  A_input: Stopping the timer.\n");
                stoptimer(A);

            }

        }

    }

}

/* called when A's timer goes off */
void A_timerinterrupt(void)
{
    printf("  A_timerinterrupt: Timer from A has timed out. Resending the frame with msg: %s with checksum: %d to medium.\n", stored_frame_A.payload, stored_frame_A.checksum);
    tolayer1(A, stored_frame_A);
    printf("  A_timerinterrupt: Starting the timer again with time units: %d.\n", timer_length);
    starttimer(A, timer_length);
}

/* the following routine will be called once (only) before any other */
/* entity A routines are called. You can use it to do any initialization */
void A_init(void)
{
    data_frame_no_A = 1;
    is_medium_busy = 0;
    acknowledgement_frame_no_A = 0;
}

/* Note that with simplex transfer from a-to-B, there is no B_output() */

/* called from layer 3, when a frame arrives for layer 4 at B*/
void B_input(struct frm frame)
{
    printf("  B_input: Received new frame in B. Updating medium status.\n");
    is_medium_busy = 0;
    if (frame.typeOfFrame == 0)
    {
        if (frame.checksum != calculate_checksum(frame))
        {
            printf("  B_input: Received corrupt frame: %d with msg: %s and checksum: %d and calculated checksum: %d from medium. Ignoring the frame.\n", frame.seqnum, frame.payload, frame.checksum, calculate_checksum(frame));
            struct frm acknowledgement_frame = make_acknowledgement_frame(acknowledgement_frame_no_B);
            tolayer1(B, acknowledgement_frame);
            return;
        }
        else if (frame.seqnum != acknowledgement_frame_no_B + 1)
        {
            printf("  B_input: Received duplicate frame: %d with msg: %s and checksum: %d from medium. Resending the previously received frame acknowledgement.\n", frame.seqnum, frame.payload, frame.checksum);
            if (piggybacked == 1)
            {
                outstandingACK_B = 0;
            }
            struct frm acknowledgement_frame = make_acknowledgement_frame(acknowledgement_frame_no_B);
            tolayer1(B, acknowledgement_frame);
            return;
        }
        else {
            printf("  B_input: Received valid frame: %d with msg: %s and checksum: %d from medium. ", frame.seqnum, frame.payload, frame.checksum);

            ++acknowledgement_frame_no_B;
        }
        if (piggybacked == 1)
        {
            printf("Awaiting piggybacked frame acknowledgement.\n");
            outstandingACK_B = 1;
        } 
        else
        {
            printf("Sending new frame acknowledgement\n");
            struct frm acknowledgement_frame = make_acknowledgement_frame(acknowledgement_frame_no_B);
            tolayer1(B, acknowledgement_frame);
        }
    }

    else if (frame.typeOfFrame == 1)
    {
        if (frame.checksum != calculate_checksum(frame))
        {
            printf("  B_input: Received corrupt acknowledgement of frame: %d with checksum: %d and calculated checksum: %d from medium. Ignoring the frame.\n", frame.acknum, frame.checksum, calculate_checksum(frame));
            return;
        }
        else if (frame.acknum != data_frame_no_B)
        {
            printf("  B_input: Received duplicate acknowledgement of frame: %d from medium. Ignoring the frame.\n", frame.acknum);
            return;
        }
        else {
            printf("  B_input: Received acknowledgement of frame: %d from medium. Updating the frame no. and medium status.\n", frame.acknum);

            ++data_frame_no_B;

            printf("  B_input: Stopping the timer.\n");
            stoptimer(B);
        }

    }

    else if (frame.typeOfFrame == 2)
    {
        if (frame.checksum != calculate_checksum(frame))
        {
            printf("  B_input: Received corrupt frame: %d with msg: %s and checksum: %d and calculated checksum: %d from medium. Ignoring the frame.\n", frame.seqnum, frame.payload, frame.checksum, calculate_checksum(frame));
            return;
            
        }
        if (frame.seqnum != acknowledgement_frame_no_B + 1)
        {
            printf("  B_input: Received duplicate data from frame: %d with msg: %s and checksum: %d from medium. Sending previous acknowledgement.\n", frame.seqnum, frame.payload, frame.checksum);
            
            if (frame.acknum != data_frame_no_B)
            {
                printf("  B_input: Received duplicate acknowledgement of frame: %d from medium. Ignoring the acknowledgement.\n", frame.acknum);
            }
            else {
                printf("  B_input: Received valid acknowledgement of frame: %d from medium.\n", frame.acknum);

                ++data_frame_no_B;

                printf("  B_input: Stopping the timer.\n");
                stoptimer(B);

            }
            outstandingACK_B = 0;
            struct frm acknowledgement_frame = make_acknowledgement_frame(acknowledgement_frame_no_B);
            tolayer1(B, acknowledgement_frame);
        }
        else
        {
            printf("  B_input: Received valid frame: %d with msg: %s and checksum: %d from medium. Sending new frame acknowledgement.\n", frame.seqnum, frame.payload, frame.checksum);

            ++acknowledgement_frame_no_B;
            outstandingACK_B = 1;
            if (frame.acknum != data_frame_no_B)
            {
                printf("  B_input: Received duplicate acknowledgement of frame: %d from medium. Ignoring the frame.\n", frame.acknum);
            }
            else {
                printf("  B_input: Received valid acknowledgement of frame: %d from medium.\n", frame.acknum);

                ++data_frame_no_B;

                printf("  B_input: Stopping the timer.\n");
                stoptimer(B);

            }

        }

    }

}

/* called when B's timer goes off */
void B_timerinterrupt(void)
{
    printf("  B_timerinterrupt: Timer from B has timed out. Resending the frame with msg: %s with checksum: %d to medium.\n", stored_frame_B.payload, stored_frame_B.checksum);
    tolayer1(B, stored_frame_B);
    printf("  B_timerinterrupt: Starting the timer again with time units: %d.\n", timer_length);
    starttimer(B, timer_length);
}

/* the following rouytine will be called once (only) before any other */
/* entity B routines are called. You can use it to do any initialization */
void B_init(void)
{
    data_frame_no_B = 1;
    acknowledgement_frame_no_B = 0;
}

/*****************************************************************
***************** NETWORK EMULATION CODE STARTS BELOW ***********
The code below emulates the layer 3 and below network environment:
    - emulates the tranmission and delivery (possibly with bit-level corruption
        and packet loss) of packets across the layer 3/4 interface
    - handles the starting/stopping of a timer, and generates timer
        interrupts (resulting in calling students timer handler).
    - generates packet to be sent (passed from later 5 to 4)

THERE IS NOT REASON THAT ANY STUDENT SHOULD HAVE TO READ OR UNDERSTAND
THE CODE BELOW.  YOU SHOLD NOT TOUCH, OR REFERENCE (in your code) ANY
OF THE DATA STRUCTURES BELOW.  If you're interested in how I designed
the emulator, you're welcome to look at the code - but again, you should have
to, and you defeinitely should not have to modify
******************************************************************/

struct event
{
    float evtime;       /* event time */
    int evtype;         /* event type code */
    int eventity;       /* entity where event occurs */
    struct frm *frmptr; /* ptr to packet (if any) assoc w/ this event */
    struct event *prev;
    struct event *next;
};
struct event *evlist = NULL; /* the event list */

/* possible events: */
#define TIMER_INTERRUPT 0
#define FROM_LAYER3 1
#define FROM_LAYER1 2

#define OFF 0
#define ON 1
#define A 0
#define B 1

int TRACE = 1;     /* for my debugging */
int nsim = 0;      /* number of packets from 5 to 4 so far */
int nsimmax = 0;   /* number of msgs to generate, then stop */
float time = 0.000;
float lossprob;    /* probability that a packet is dropped  */
float corruptprob; /* probability that one bit is packet is flipped */
float lambda;      /* arrival rate of packets from layer 5 */
int ntolayer1;     /* number sent into layer 3 */
int nlost;         /* number lost in media */
int ncorrupt;      /* number corrupted by media*/

void init();
void generate_next_arrival(void);
void insertevent(struct event *p);

int main()
{
    struct event *eventptr;
    struct pkt pkt2give;
    struct frm frm2give;

    int i, j;
    char c;

    init();
    A_init();
    B_init();

    while (1)
    {
        eventptr = evlist; /* get next event to simulate */
        if (eventptr == NULL)
            goto terminate;
        evlist = evlist->next; /* remove this event from event list */
        if (evlist != NULL)
            evlist->prev = NULL;
        if (TRACE >= 2)
        {
            printf("\nEVENT time: %f,", eventptr->evtime);
            printf("  type: %d", eventptr->evtype);
            if (eventptr->evtype == 0)
                printf(", timerinterrupt  ");
            else if (eventptr->evtype == 1)
                printf(", fromlayer5 ");
            else
                printf(", fromlayer3 ");
            printf(" entity: %d\n", eventptr->eventity);
        }
        time = eventptr->evtime; /* update time to next event time */
        if (eventptr->evtype == FROM_LAYER3)
        {
            if (nsim < nsimmax)
            {
                if (nsim + 1 < nsimmax)
                    generate_next_arrival(); /* set up future arrival */
                /* fill in msg to give with string of same letter */
                j = nsim % 26;
                for (i = 0; i < payload_size; i++)
                    pkt2give.data[i] = 97 + j;
                pkt2give.data[19] = 0;
                if (TRACE > 2)
                {
                    printf("          MAINLOOP: data given to student: ");
                    for (i = 0; i < payload_size; i++)
                        printf("%c", pkt2give.data[i]);
                    printf("\n");
                }
                nsim++;
                if (eventptr->eventity == A)
                    A_output(pkt2give);
                else
                    B_output(pkt2give);
            }
        }
        else if (eventptr->evtype == FROM_LAYER1)
        {
            frm2give.seqnum = eventptr->frmptr->seqnum;
            frm2give.acknum = eventptr->frmptr->acknum;
            frm2give.checksum = eventptr->frmptr->checksum;
            frm2give.typeOfFrame = eventptr->frmptr->typeOfFrame;
            for (i = 0; i < payload_size; i++)
                frm2give.payload[i] = eventptr->frmptr->payload[i];
            if (eventptr->eventity == A) /* deliver packet by calling */
                A_input(frm2give); /* appropriate entity */
            else
                B_input(frm2give);
            free(eventptr->frmptr); /* free the memory for packet */
        }
        else if (eventptr->evtype == TIMER_INTERRUPT)
        {
            if (eventptr->eventity == A)
                A_timerinterrupt();
            else
                B_timerinterrupt();
        }
        else
        {
            printf("INTERNAL PANIC: unknown event type \n");
        }
        free(eventptr);
    }

terminate:
    printf(
        " Simulator terminated at time %f\n after sending %d msgs from layer5\n",
        time, nsim);
}

void init() /* initialize the simulator */
{
    int i;
    float sum, avg;
    float jimsrand();

    printf("-----  Stop and Wait Network Simulator Version 1.1 -------- \n\n");
    printf("Enter the number of frame to simulate: ");
    scanf("%d",&nsimmax);
    printf("Enter 1 for piggybacked acknowledgement or 0 otherwise: ");
    scanf("%d",&piggybacked);
    printf("Enter frame loss probability [enter 0.0 for no loss]: ");
    scanf("%f",&lossprob);
    printf("Enter frame corruption probability [0.0 for no corruption]: ");
    scanf("%f",&corruptprob);
    printf("Enter average time between frames from sender's layer5 [ > 0.0]: ");
    scanf("%f",&lambda);
    printf("Enter TRACE: ");
    scanf("%d",&TRACE);

    srand(9999); /* init random number generator */
    sum = 0.0;   /* test random number generator for students */
    for (i = 0; i < 1000; i++)
        sum = sum + jimsrand(); /* jimsrand() should be uniform in [0,1] */
    avg = sum / 1000.0;
    if (avg < 0.25 || avg > 0.75)
    {
        printf("It is likely that random number generation on your machine\n");
        printf("is different from what this emulator expects.  Please take\n");
        printf("a look at the routine jimsrand() in the emulator code. Sorry. \n");
        exit(1);
    }

    ntolayer1 = 0;
    nlost = 0;
    ncorrupt = 0;

    time = 0.0;              /* initialize time to 0.0 */
    generate_next_arrival(); /* initialize event list */
}

/****************************************************************************/
/* jimsrand(): return a float in range [0,1].  The routine below is used to */
/* isolate all random number generation in one location.  We assume that the*/
/* system-supplied rand() function return an int in therange [0,mmm]        */
/****************************************************************************/
float jimsrand(void)
{
    double mmm = RAND_MAX;
    float x;                 /* individual students may need to change mmm */
    x = rand() / mmm;        /* x should be uniform in [0,1] */
    return (x);
}

/********************* EVENT HANDLINE ROUTINES *******/
/*  The next set of routines handle the event list   */
/*****************************************************/

void generate_next_arrival(void)
{
    double x, log(), ceil();
    struct event *evptr;
    float ttime;
    int tempint;

    if (TRACE > 2)
        printf("          GENERATE NEXT ARRIVAL: creating new arrival\n");

    x = lambda * jimsrand() * 2; /* x is uniform on [0,2*lambda] */
    /* having mean of lambda        */
    evptr = (struct event *)malloc(sizeof(struct event));
    evptr->evtime = time + x;
    evptr->evtype = FROM_LAYER3;
    if (BIDIRECTIONAL && (jimsrand() > 0.5))
        evptr->eventity = B;
    else
        evptr->eventity = A;
    insertevent(evptr);
}

void insertevent(struct event *p)
{
    struct event *q, *qold;

    if (TRACE > 2)
    {
        printf("            INSERTEVENT: time is %lf\n", time);
        printf("            INSERTEVENT: future time will be %lf\n", p->evtime);
    }
    q = evlist;      /* q points to header of list in which p struct inserted */
    if (q == NULL)   /* list is empty */
    {
        evlist = p;
        p->next = NULL;
        p->prev = NULL;
    }
    else
    {
        for (qold = q; q != NULL && p->evtime > q->evtime; q = q->next)
            qold = q;
        if (q == NULL)   /* end of list */
        {
            qold->next = p;
            p->prev = qold;
            p->next = NULL;
        }
        else if (q == evlist)     /* front of list */
        {
            p->next = evlist;
            p->prev = NULL;
            p->next->prev = p;
            evlist = p;
        }
        else     /* middle of list */
        {
            p->next = q;
            p->prev = q->prev;
            q->prev->next = p;
            q->prev = p;
        }
    }
}

void printevlist(void)
{
    struct event *q;
    int i;
    printf("--------------\nEvent List Follows:\n");
    for (q = evlist; q != NULL; q = q->next)
    {
        printf("Event time: %f, type: %d entity: %d\n", q->evtime, q->evtype,
               q->eventity);
    }
    printf("--------------\n");
}

/********************** Student-callable ROUTINES ***********************/

/* called by students routine to cancel a previously-started timer */
void stoptimer(int AorB /* A or B is trying to stop timer */)
{
    struct event *q, *qold;

    if (TRACE > 2)
        printf("          STOP TIMER: stopping timer at %f\n", time);
    /* for (q=evlist; q!=NULL && q->next!=NULL; q = q->next)  */
    for (q = evlist; q != NULL; q = q->next)
        if ((q->evtype == TIMER_INTERRUPT && q->eventity == AorB))
        {
            /* remove this event */
            if (q->next == NULL && q->prev == NULL)
                evlist = NULL;          /* remove first and only event on list */
            else if (q->next == NULL) /* end of list - there is one in front */
                q->prev->next = NULL;
            else if (q == evlist)   /* front of list - there must be event after */
            {
                q->next->prev = NULL;
                evlist = q->next;
            }
            else     /* middle of list */
            {
                q->next->prev = q->prev;
                q->prev->next = q->next;
            }
            free(q);
            return;
        }
    printf("Warning: unable to cancel your timer. It wasn't running.\n");
}

void starttimer(int AorB /* A or B is trying to start timer */, float increment)
{
    struct event *q;
    struct event *evptr;

    if (TRACE > 2)
        printf("          START TIMER: starting timer at %f\n", time);
    /* be nice: check to see if timer is already started, if so, then  warn */
    /* for (q=evlist; q!=NULL && q->next!=NULL; q = q->next)  */
    for (q = evlist; q != NULL; q = q->next)
        if ((q->evtype == TIMER_INTERRUPT && q->eventity == AorB))
        {
            printf("Warning: attempt to start a timer that is already started\n");
            return;
        }

    /* create future event for when timer goes off */
    evptr = (struct event *)malloc(sizeof(struct event));
    evptr->evtime = time + increment;
    evptr->evtype = TIMER_INTERRUPT;
    evptr->eventity = AorB;
    insertevent(evptr);
}

/************************** tolayer1 ***************/
void tolayer1(int AorB, struct frm frame)
{
    struct frm *myfrmptr;
    struct event *evptr, *q;
    float lastime, x;
    int i;

    ntolayer1++;

    /* simulate losses: */
    if (jimsrand() < lossprob)
    {
        nlost++;
        if (TRACE > 0)
            printf("          tolayer1: frame being lost\n");
        return;
    }

    /* make a copy of the packet student just gave me since he/she may decide */
    /* to do something with the packet after we return back to him/her */
    myfrmptr = (struct frm *)malloc(sizeof(struct frm));
    myfrmptr->seqnum = frame.seqnum;
    myfrmptr->acknum = frame.acknum;
    myfrmptr->checksum = frame.checksum;
     myfrmptr->typeOfFrame = frame.typeOfFrame;
    for (i = 0; i < payload_size; i++)
        myfrmptr->payload[i] = frame.payload[i];
    if (TRACE > 2)
    {
        printf("          tolayer1: seq: %d, ack %d, check: %d ", myfrmptr->seqnum,
               myfrmptr->acknum, myfrmptr->checksum);
        for (i = 0; i < payload_size; i++)
            printf("%c", myfrmptr->payload[i]);
        printf("\n");
    }

    /* create future event for arrival of packet at the other side */
    evptr = (struct event *)malloc(sizeof(struct event));
    evptr->evtype = FROM_LAYER1;      /* packet will pop out from layer3 */
    evptr->eventity = (AorB + 1) % 2; /* event occurs at other entity */
    evptr->frmptr = myfrmptr;         /* save ptr to my copy of packet */
    /* finally, compute the arrival time of packet at the other end.
       medium can not reorder, so make sure packet arrives between 1 and 10
       time units after the latest arrival time of packets
       currently in the medium on their way to the destination */
    lastime = time;
    /* for (q=evlist; q!=NULL && q->next!=NULL; q = q->next) */
    for (q = evlist; q != NULL; q = q->next)
        if ((q->evtype == FROM_LAYER1 && q->eventity == evptr->eventity))
            lastime = q->evtime;
    evptr->evtime = lastime + 1 + 9 * jimsrand();

    /* simulate corruption: */
    if (jimsrand() < corruptprob)
    {
        ncorrupt++;
        if ((x = jimsrand()) < .75)
            myfrmptr->payload[0] = 'Z'; /* corrupt payload */
        else if (x < .875)
            myfrmptr->seqnum = 999999;
        else
            myfrmptr->acknum = 999999;
        if (TRACE > 0)
            printf("          tolayer1: frame being corrupted\n");
    }

    if (TRACE > 2)
        printf("          tolayer1: scheduling arrival on other side\n");
    insertevent(evptr);
}

void tolayer3(int AorB, char datasent[payload_size])
{
    int i;
    if (TRACE > 2)
    {
        printf("          tolayer3: data received: ");
        for (i = 0; i < payload_size; i++)
            printf("%c", datasent[i]);
        printf("\n");
    }
}
