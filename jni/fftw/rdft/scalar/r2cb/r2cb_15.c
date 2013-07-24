/*
 * Copyright (c) 2003, 2007-11 Matteo Frigo
 * Copyright (c) 2003, 2007-11 Massachusetts Institute of Technology
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

/* This file was automatically generated --- DO NOT EDIT */
/* Generated on Sun Nov 25 07:41:08 EST 2012 */

#include "codelet-rdft.h"

#ifdef HAVE_FMA

/* Generated by: ../../../genfft/gen_r2cb.native -fma -reorder-insns -schedule-for-pipeline -compact -variables 4 -pipeline-latency 4 -sign 1 -n 15 -name r2cb_15 -include r2cb.h */

/*
 * This function contains 64 FP additions, 43 FP multiplications,
 * (or, 21 additions, 0 multiplications, 43 fused multiply/add),
 * 54 stack variables, 9 constants, and 30 memory accesses
 */
#include "r2cb.h"

static void r2cb_15(R *R0, R *R1, R *Cr, R *Ci, stride rs, stride csr, stride csi, INT v, INT ivs, INT ovs)
{
     DK(KP559016994, +0.559016994374947424102293417182819058860154590);
     DK(KP1_902113032, +1.902113032590307144232878666758764286811397268);
     DK(KP250000000, +0.250000000000000000000000000000000000000000000);
     DK(KP866025403, +0.866025403784438646763723170752936183471402627);
     DK(KP1_118033988, +1.118033988749894848204586834365638117720309180);
     DK(KP618033988, +0.618033988749894848204586834365638117720309180);
     DK(KP500000000, +0.500000000000000000000000000000000000000000000);
     DK(KP1_732050807, +1.732050807568877293527446341505872366942805254);
     DK(KP2_000000000, +2.000000000000000000000000000000000000000000000);
     {
	  INT i;
	  for (i = v; i > 0; i = i - 1, R0 = R0 + ovs, R1 = R1 + ovs, Cr = Cr + ivs, Ci = Ci + ivs, MAKE_VOLATILE_STRIDE(60, rs), MAKE_VOLATILE_STRIDE(60, csr), MAKE_VOLATILE_STRIDE(60, csi)) {
	       E TL, Tz, TM, TK;
	       {
		    E T3, Th, Tt, TD, TI, TH, TY, TC, TZ, Tu, Tm, Tv, Tr, Te, TW;
		    E Tg, T1, T2, T12, T10, TV;
		    Tg = Ci[WS(csi, 5)];
		    T1 = Cr[0];
		    T2 = Cr[WS(csr, 5)];
		    {
			 E T4, TA, T9, TF, T7, Tj, Tc, Tk, TG, Tq, Tf, Tl, TB;
			 T4 = Cr[WS(csr, 3)];
			 TA = Ci[WS(csi, 3)];
			 T9 = Cr[WS(csr, 6)];
			 Tf = T1 - T2;
			 T3 = FMA(KP2_000000000, T2, T1);
			 TF = Ci[WS(csi, 6)];
			 {
			      E Ta, Tb, T5, T6, To, Tp;
			      T5 = Cr[WS(csr, 7)];
			      T6 = Cr[WS(csr, 2)];
			      Th = FMA(KP1_732050807, Tg, Tf);
			      Tt = FNMS(KP1_732050807, Tg, Tf);
			      Ta = Cr[WS(csr, 4)];
			      TD = T5 - T6;
			      T7 = T5 + T6;
			      Tb = Cr[WS(csr, 1)];
			      To = Ci[WS(csi, 4)];
			      Tp = Ci[WS(csi, 1)];
			      Tj = Ci[WS(csi, 7)];
			      Tc = Ta + Tb;
			      TI = Ta - Tb;
			      Tk = Ci[WS(csi, 2)];
			      TG = Tp - To;
			      Tq = To + Tp;
			 }
			 Tl = Tj - Tk;
			 TB = Tj + Tk;
			 TH = FNMS(KP500000000, TG, TF);
			 TY = TG + TF;
			 TC = FMA(KP500000000, TB, TA);
			 TZ = TA - TB;
			 {
			      E Ti, T8, Td, Tn;
			      Ti = FNMS(KP2_000000000, T4, T7);
			      T8 = T4 + T7;
			      Td = T9 + Tc;
			      Tn = FNMS(KP2_000000000, T9, Tc);
			      Tu = FNMS(KP1_732050807, Tl, Ti);
			      Tm = FMA(KP1_732050807, Tl, Ti);
			      Tv = FNMS(KP1_732050807, Tq, Tn);
			      Tr = FMA(KP1_732050807, Tq, Tn);
			      Te = T8 + Td;
			      TW = T8 - Td;
			 }
		    }
		    T12 = FMA(KP618033988, TY, TZ);
		    T10 = FNMS(KP618033988, TZ, TY);
		    TV = FNMS(KP500000000, Te, T3);
		    R0[0] = FMA(KP2_000000000, Te, T3);
		    {
			 E TJ, TE, TT, TP, TU, TS, Ty, Tw, Tx;
			 {
			      E TO, Ts, TQ, TN, TR, T11, TX;
			      TO = Tr - Tm;
			      Ts = Tm + Tr;
			      T11 = FMA(KP1_118033988, TW, TV);
			      TX = FNMS(KP1_118033988, TW, TV);
			      TQ = FNMS(KP866025403, TI, TH);
			      TJ = FMA(KP866025403, TI, TH);
			      TN = FMA(KP250000000, Ts, Th);
			      R0[WS(rs, 3)] = FNMS(KP1_902113032, T12, T11);
			      R1[WS(rs, 4)] = FMA(KP1_902113032, T12, T11);
			      R0[WS(rs, 6)] = FMA(KP1_902113032, T10, TX);
			      R1[WS(rs, 1)] = FNMS(KP1_902113032, T10, TX);
			      TR = FNMS(KP866025403, TD, TC);
			      TE = FMA(KP866025403, TD, TC);
			      R1[WS(rs, 2)] = Th - Ts;
			      TT = FMA(KP559016994, TO, TN);
			      TP = FNMS(KP559016994, TO, TN);
			      TU = FMA(KP618033988, TQ, TR);
			      TS = FNMS(KP618033988, TR, TQ);
			 }
			 Ty = Tv - Tu;
			 Tw = Tu + Tv;
			 R0[WS(rs, 7)] = FMA(KP1_902113032, TU, TT);
			 R1[WS(rs, 5)] = FNMS(KP1_902113032, TU, TT);
			 R0[WS(rs, 1)] = FMA(KP1_902113032, TS, TP);
			 R0[WS(rs, 4)] = FNMS(KP1_902113032, TS, TP);
			 Tx = FMA(KP250000000, Tw, Tt);
			 R0[WS(rs, 5)] = Tt - Tw;
			 TL = FNMS(KP559016994, Ty, Tx);
			 Tz = FMA(KP559016994, Ty, Tx);
			 TM = FNMS(KP618033988, TE, TJ);
			 TK = FMA(KP618033988, TJ, TE);
		    }
	       }
	       R1[WS(rs, 3)] = FMA(KP1_902113032, TM, TL);
	       R1[WS(rs, 6)] = FNMS(KP1_902113032, TM, TL);
	       R0[WS(rs, 2)] = FMA(KP1_902113032, TK, Tz);
	       R1[0] = FNMS(KP1_902113032, TK, Tz);
	  }
     }
}

static const kr2c_desc desc = { 15, "r2cb_15", {21, 0, 43, 0}, &GENUS };

void X(codelet_r2cb_15) (planner *p) {
     X(kr2c_register) (p, r2cb_15, &desc);
}

#else				/* HAVE_FMA */

/* Generated by: ../../../genfft/gen_r2cb.native -compact -variables 4 -pipeline-latency 4 -sign 1 -n 15 -name r2cb_15 -include r2cb.h */

/*
 * This function contains 64 FP additions, 31 FP multiplications,
 * (or, 47 additions, 14 multiplications, 17 fused multiply/add),
 * 44 stack variables, 7 constants, and 30 memory accesses
 */
#include "r2cb.h"

static void r2cb_15(R *R0, R *R1, R *Cr, R *Ci, stride rs, stride csr, stride csi, INT v, INT ivs, INT ovs)
{
     DK(KP1_118033988, +1.118033988749894848204586834365638117720309180);
     DK(KP1_902113032, +1.902113032590307144232878666758764286811397268);
     DK(KP1_175570504, +1.175570504584946258337411909278145537195304875);
     DK(KP500000000, +0.500000000000000000000000000000000000000000000);
     DK(KP866025403, +0.866025403784438646763723170752936183471402627);
     DK(KP2_000000000, +2.000000000000000000000000000000000000000000000);
     DK(KP1_732050807, +1.732050807568877293527446341505872366942805254);
     {
	  INT i;
	  for (i = v; i > 0; i = i - 1, R0 = R0 + ovs, R1 = R1 + ovs, Cr = Cr + ivs, Ci = Ci + ivs, MAKE_VOLATILE_STRIDE(60, rs), MAKE_VOLATILE_STRIDE(60, csr), MAKE_VOLATILE_STRIDE(60, csi)) {
	       E T3, Tu, Ti, TB, TZ, T10, TE, TG, TJ, Tn, Tv, Ts, Tw, T8, Td;
	       E Te;
	       {
		    E Th, T1, T2, Tf, Tg;
		    Tg = Ci[WS(csi, 5)];
		    Th = KP1_732050807 * Tg;
		    T1 = Cr[0];
		    T2 = Cr[WS(csr, 5)];
		    Tf = T1 - T2;
		    T3 = FMA(KP2_000000000, T2, T1);
		    Tu = Tf - Th;
		    Ti = Tf + Th;
	       }
	       {
		    E T4, TD, T9, TI, T5, T6, T7, Ta, Tb, Tc, Tr, TH, Tm, TC, Tj;
		    E To;
		    T4 = Cr[WS(csr, 3)];
		    TD = Ci[WS(csi, 3)];
		    T9 = Cr[WS(csr, 6)];
		    TI = Ci[WS(csi, 6)];
		    T5 = Cr[WS(csr, 7)];
		    T6 = Cr[WS(csr, 2)];
		    T7 = T5 + T6;
		    Ta = Cr[WS(csr, 4)];
		    Tb = Cr[WS(csr, 1)];
		    Tc = Ta + Tb;
		    {
			 E Tp, Tq, Tk, Tl;
			 Tp = Ci[WS(csi, 4)];
			 Tq = Ci[WS(csi, 1)];
			 Tr = KP866025403 * (Tp + Tq);
			 TH = Tp - Tq;
			 Tk = Ci[WS(csi, 7)];
			 Tl = Ci[WS(csi, 2)];
			 Tm = KP866025403 * (Tk - Tl);
			 TC = Tk + Tl;
		    }
		    TB = KP866025403 * (T5 - T6);
		    TZ = TD - TC;
		    T10 = TI - TH;
		    TE = FMA(KP500000000, TC, TD);
		    TG = KP866025403 * (Ta - Tb);
		    TJ = FMA(KP500000000, TH, TI);
		    Tj = FNMS(KP500000000, T7, T4);
		    Tn = Tj - Tm;
		    Tv = Tj + Tm;
		    To = FNMS(KP500000000, Tc, T9);
		    Ts = To - Tr;
		    Tw = To + Tr;
		    T8 = T4 + T7;
		    Td = T9 + Tc;
		    Te = T8 + Td;
	       }
	       R0[0] = FMA(KP2_000000000, Te, T3);
	       {
		    E T11, T13, TY, T12, TW, TX;
		    T11 = FNMS(KP1_902113032, T10, KP1_175570504 * TZ);
		    T13 = FMA(KP1_902113032, TZ, KP1_175570504 * T10);
		    TW = FNMS(KP500000000, Te, T3);
		    TX = KP1_118033988 * (T8 - Td);
		    TY = TW - TX;
		    T12 = TX + TW;
		    R0[WS(rs, 6)] = TY - T11;
		    R1[WS(rs, 4)] = T12 + T13;
		    R1[WS(rs, 1)] = TY + T11;
		    R0[WS(rs, 3)] = T12 - T13;
	       }
	       {
		    E TP, Tt, TO, TT, TV, TR, TS, TU, TQ;
		    TP = KP1_118033988 * (Tn - Ts);
		    Tt = Tn + Ts;
		    TO = FNMS(KP500000000, Tt, Ti);
		    TR = TE - TB;
		    TS = TJ - TG;
		    TT = FNMS(KP1_902113032, TS, KP1_175570504 * TR);
		    TV = FMA(KP1_902113032, TR, KP1_175570504 * TS);
		    R1[WS(rs, 2)] = FMA(KP2_000000000, Tt, Ti);
		    TU = TP + TO;
		    R1[WS(rs, 5)] = TU - TV;
		    R0[WS(rs, 7)] = TU + TV;
		    TQ = TO - TP;
		    R0[WS(rs, 1)] = TQ - TT;
		    R0[WS(rs, 4)] = TQ + TT;
	       }
	       {
		    E Tz, Tx, Ty, TL, TN, TF, TK, TM, TA;
		    Tz = KP1_118033988 * (Tv - Tw);
		    Tx = Tv + Tw;
		    Ty = FNMS(KP500000000, Tx, Tu);
		    TF = TB + TE;
		    TK = TG + TJ;
		    TL = FNMS(KP1_902113032, TK, KP1_175570504 * TF);
		    TN = FMA(KP1_902113032, TF, KP1_175570504 * TK);
		    R0[WS(rs, 5)] = FMA(KP2_000000000, Tx, Tu);
		    TM = Tz + Ty;
		    R1[0] = TM - TN;
		    R0[WS(rs, 2)] = TM + TN;
		    TA = Ty - Tz;
		    R1[WS(rs, 3)] = TA - TL;
		    R1[WS(rs, 6)] = TA + TL;
	       }
	  }
     }
}

static const kr2c_desc desc = { 15, "r2cb_15", {47, 14, 17, 0}, &GENUS };

void X(codelet_r2cb_15) (planner *p) {
     X(kr2c_register) (p, r2cb_15, &desc);
}

#endif				/* HAVE_FMA */
