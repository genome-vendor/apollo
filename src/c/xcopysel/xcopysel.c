/* SMJS Hacked together a selection copier from xcutsel code */
/*
 * $XConsortium: xcutsel.c /main/21 1996/02/02 14:26:48 kaleb $
 *
 * 
Copyright (c) 1989  X Consortium

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
X CONSORTIUM BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of the X Consortium shall not be
used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from the X Consortium.
 * *
 * Author:  Ralph Swick, DEC/Project Athena
 */
/* $XFree86: xc/programs/xclipboard/xcutsel.c,v 1.1.1.3.4.2 1998/10/04 15:23:10 hohndel Exp $ */


#include <stdio.h>
#include <X11/Intrinsic.h>
#include <X11/StringDefs.h>
#include <X11/Xatom.h>

#include <X11/Xmu/Atoms.h>
#include <X11/Xmu/StdSel.h>

#include <X11/Xaw/Command.h>
#include <X11/Xaw/Box.h>
#include <X11/Xaw/Cardinals.h>
#include <X11/Xfuncs.h>

#ifdef XKB
#include <X11/extensions/XKBbells.h>
#endif

static XrmOptionDescRec optionDesc[] = {
    {"-selection", "selection", XrmoptionSepArg, NULL},
    {"-select",    "selection", XrmoptionSepArg, NULL},
    {"-sel",       "selection",        XrmoptionSepArg, NULL},
    {"-s",         "selection",        XrmoptionSepArg, NULL},
    {"-cutbuffer", "cutBuffer", XrmoptionSepArg, NULL},
    {"-selection2", "selection2", XrmoptionSepArg, NULL},
};

typedef struct {
    String  selection_name;
    int     buffer;
    Atom    selection;
    char*   value;
    int     length;
    String  selection2_name;
    Atom    selection2;
} OptionsRec;

OptionsRec options;

#define Offset(field) XtOffsetOf(OptionsRec, field)

static XtResource resources[] = {
    {"selection", "Selection", XtRString, sizeof(String),
       Offset(selection_name), XtRString, "PRIMARY"},
    {"cutBuffer", "CutBuffer", XtRInt, sizeof(int),
       Offset(buffer), XtRImmediate, (XtPointer)0},
    {"selection2", "Selection2", XtRString, sizeof(String),
       Offset(selection2_name), XtRString, "CLIPBOARD"},
};

#undef Offset

typedef struct {
    Widget button;
    Boolean is_on;
} ButtonState;

static ButtonState stateCopy;

Syntax(call)
        char *call;
{
    fprintf (stderr, "usage:  %s [-selection name] [-cutbuffer number] [-selection2 name]\n", 
             call);
    exit (1);
}


static Boolean ConvertSelection(w, selection, target,
                                type, value, length, format)
    Widget w;
    Atom *selection, *target, *type;
    XtPointer *value;
    unsigned long *length;
    int *format;
{
    Display* d = XtDisplay(w);
    XSelectionRequestEvent* req =
        XtGetSelectionRequest(w, *selection, (XtRequestId)NULL);
        
    if (*target == XA_TARGETS(d)) {
        Atom* targetP;
        Atom* std_targets;
        unsigned long std_length;
        XmuConvertStandardSelection(w, req->time, selection, target, type,
                                   (XPointer*)&std_targets, &std_length, format);
        *value = XtMalloc(sizeof(Atom)*(std_length + 4));
        targetP = *(Atom**)value;
        *length = std_length + 4;
        *targetP++ = XA_STRING;
        *targetP++ = XA_TEXT(d);
        *targetP++ = XA_LENGTH(d);
        *targetP++ = XA_LIST_LENGTH(d);
/*
        *targetP++ = XA_CHARACTER_POSITION(d);
*/
        memmove( (char*)targetP, (char*)std_targets, sizeof(Atom)*std_length);
        XtFree((char*)std_targets);
        *type = XA_ATOM;
        *format = 32;
        return True;
    }
    if (*target == XA_STRING || *target == XA_TEXT(d)) {
        *type = XA_STRING;
        *value = XtMalloc((Cardinal) options.length);
        memmove( (char *) *value, options.value, options.length);
        *length = options.length;
        *format = 8;
        return True;
    }
    if (*target == XA_LIST_LENGTH(d)) {
        long *temp = (long *) XtMalloc (sizeof(long));
        *temp = 1L;
        *value = (XtPointer) temp;
        *type = XA_INTEGER;
        *length = 1;
        *format = 32;
        return True;
    }
    if (*target == XA_LENGTH(d)) {
        long *temp = (long *) XtMalloc (sizeof(long));
        *temp = options.length;
        *value = (XtPointer) temp;
        *type = XA_INTEGER;
        *length = 1;
        *format = 32;
        return True;
    }
#ifdef notdef
    if (*target == XA_CHARACTER_POSITION(d)) {
        long *temp = (long *) XtMalloc (2 * sizeof(long));
        temp[0] = ctx->text.s.left + 1;
        temp[1] = ctx->text.s.right;
        *value = (XtPointer) temp;
        *type = XA_SPAN(d);
        *length = 2;
        *format = 32;
        return True;
    }
#endif /* notdef */
    if (XmuConvertStandardSelection(w, req->time, selection, target, type,
                                    (XPointer *)value, length, format))
        return True;

    /* else */
    return False;
}


static void SetButton(state, on)
    ButtonState *state;
    Boolean on;
{
    if (state->is_on != on) {
        Arg args[2];
        Pixel fg, bg;
        XtSetArg( args[0], XtNforeground, &fg );
        XtSetArg( args[1], XtNbackground, &bg );
        XtGetValues( state->button, args, TWO );
        args[0].value = (XtArgVal)bg;
        args[1].value = (XtArgVal)fg;
        XtSetValues( state->button, args, TWO );
        state->is_on = on;
    }
}



static void LoseCopySelection(w, selection)
    Widget w;
    Atom *selection;
{
    if (options.value) {
        XFree( options.value );
        options.value = NULL;
    }
    SetButton(&stateCopy, False);
}


/* ARGSUSED */
static void Quit(w, closure, callData)
    Widget w;
    XtPointer closure;          /* unused */
    XtPointer callData;         /* unused */
{
    XtCloseDisplay( XtDisplay(w) );
    exit(0);
}

static void StoreCopyBuffer(
    Widget w,
    XtPointer client_data,
    Atom *selection, Atom *type,
    XtPointer value,
    unsigned long *length,
    int *format);

static void
LoseClipSelection(Widget w, Atom *selection)
{
    XtGetSelectionValue(w, *selection, XA_STRING, StoreCopyBuffer,
                        NULL, CurrentTime);
}

static void StoreCopyBuffer(w, client_data, selection, type, value, length, format)
    Widget w;
    XtPointer client_data;
    Atom *selection, *type;
    XtPointer value;
    unsigned long *length;
    int *format;
{

    if (*type == 0 || *type == XT_CONVERT_FAIL || *length == 0) {
#ifdef XKB
        XkbStdBell( XtDisplay(w), XtWindow(w), 0, XkbBI_MinorError );
#else
        XBell( XtDisplay(w), 0 );
#endif
        return;
    }

    XStoreBuffer( XtDisplay(w), (char*)value, (int)(*length),
                  options.buffer );

    XtFree(value);

/* Make buffer available as selection */
    if (options.value) XFree( options.value );
    options.value =
        XFetchBuffer(XtDisplay(w), &options.length, options.buffer);
    if (options.value != NULL) {
       if (XtOwnSelection(w, options.selection,
                          XtLastTimestampProcessed(XtDisplay(w)),
                          ConvertSelection, LoseCopySelection, NULL)) {
       }
       XtOwnSelection(w, options.selection2, 
                      XtLastTimestampProcessed(XtDisplay(w)),
                      ConvertSelection, LoseClipSelection, NULL);
    } else {
       printf("ERROR: NULL value\n");
    }
}


static void CopySelection(w, closure, callData)
    Widget w;
    XtPointer closure;          /* unused */
    XtPointer callData;         /* unused */
{
/* Copy from selection2 into buffer */
    XtGetSelectionValue(w, options.selection2, XA_STRING,
                        StoreCopyBuffer, NULL,
                        XtLastTimestampProcessed(XtDisplay(w)));
    SetButton((ButtonState*)closure, True);
}


RefuseSelection(Widget w, Atom *selection, Atom *target,
                Atom *type, XtPointer *value, unsigned long *length,
                int *format)
{
    return False;
}

static void
LoseManager(Widget w, Atom *selection)
{
    XtError("another clipboard has taken over control\n");
}


int main(argc, argv)
    int argc;
    char *argv[];
{
    char label[100];
    Widget box, button;
    XtAppContext appcon;
    Widget shell;
    XrmDatabase rdb;
    Atom ManagerAtom;

    XtSetLanguageProc(NULL, NULL, NULL);


    shell =
        XtAppInitialize( &appcon, "XCutsel", optionDesc, XtNumber(optionDesc),
                         &argc, argv, NULL, NULL, 0 );
    rdb = XtDatabase(XtDisplay(shell));
    ManagerAtom = XInternAtom(XtDisplay(shell), "CLIPBOARD_MANAGER", False);
    if (XGetSelectionOwner(XtDisplay(shell), ManagerAtom))
        XtError("another copier is already running\n");



    if (argc != 1) Syntax(argv[0]);

    XtGetApplicationResources( shell, (XtPointer)&options,
                               resources, XtNumber(resources),
                               NULL, ZERO );

    options.value = NULL;
    XmuInternStrings( XtDisplay(shell), &options.selection_name, ONE,
                      &options.selection );

    XmuInternStrings( XtDisplay(shell), &options.selection2_name, ONE,
                      &options.selection2 );

    box = XtCreateManagedWidget("box", boxWidgetClass, shell, NULL, ZERO);

    button =
        XtCreateManagedWidget("quit", commandWidgetClass, box, NULL, ZERO);
        XtAddCallback( button, XtNcallback, Quit, NULL );

    /* %%% hack alert... */
    sprintf(label, "*label:copy %s to %s",
            options.selection2_name,
            options.selection_name);
    XrmPutLineResource( &rdb, label );

    button =
        XtCreateManagedWidget("sel-sel", commandWidgetClass, box, NULL, ZERO);
        XtAddCallback( button, XtNcallback, CopySelection, (XtPointer)&stateCopy );
        stateCopy.button = button;
        stateCopy.is_on = False;
   
    XtRealizeWidget(shell);

    XtOwnSelection(shell, ManagerAtom, CurrentTime,
                   RefuseSelection, LoseManager, NULL);
    if (XGetSelectionOwner (XtDisplay(shell), options.selection2)) {
        LoseClipSelection (shell, &options.selection2);
    } else {
        XtOwnSelection(shell, options.selection2, CurrentTime,
                       ConvertSelection, LoseClipSelection, NULL);
    }
    XtAppMainLoop(appcon);
}
