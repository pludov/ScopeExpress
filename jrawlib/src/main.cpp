/*
 * main.cpp
 *
 *  Created on: 18 août 2012
 *      Author: utilisateur
 */

#include <jni.h>
#include "fr_pludov_io_JRawLib.h"
#include "libraw.h"


JNIEXPORT jcharArray JNICALL Java_fr_pludov_io_JRawLib_doLoad (JNIEnv * env, jobject obj, jbyteArray jpath){
	jclass cls = env->GetObjectClass(obj);


	 LibRaw * iProcessor = 0;
	 if (jpath == 0) return 0;
	 int length = env->GetArrayLength(jpath);
	 if (length == 0) return 0;
	 char * path = (char*)malloc(length + 1);
	 if (path == 0) return 0;

	 signed char * pathNoZT = env->GetByteArrayElements(jpath, 0);
	 memcpy(path, pathNoZT, length);
	 path[length] = 0;

	 try {


		 iProcessor = new LibRaw();

		 // Open the file and read the metadata
		 if (iProcessor->open_file(path) != 0) {
			 fprintf(stderr, "failed to open %s\n", path);
			 fflush(stderr);
			 throw "bye bye";
		 }


		 if (iProcessor->unpack() != 0) {
			 fprintf(stderr, "failed to open %s\n", path);
			 fflush(stderr);
			 throw "bye bye";
		 }
#define S iProcessor->imgdata.sizes
		 printf("Image size: %dx%d\nRaw size: %dx%d\n",S.width,S.height,S.raw_width,S.raw_height);
		 printf("Margins: top=%d, left=%d\n",
		                            S.top_margin,S.left_margin);
/*
 *     color_data_state_t   color_flags;
    ushort      white[8][8];
    float       cam_mul[4];
    float       pre_mul[4];
    float       cmatrix[3][4];
    float       rgb_cam[3][4];
    float       cam_xyz[4][3];
    ushort      curve[0x10000];
    unsigned    black;
    unsigned    cblack[8];
    unsigned    maximum;
    unsigned    channel_maximum[4];
    struct ph1_t       phase_one_data;
    float       flash_used;
    float       canon_ev;
    char        model2[64];
    void        *profile;
    unsigned    profile_length;
    short  (*ph1_black)[2];
 *
 */
		 jfieldID fid;


		 int black = iProcessor->imgdata.color.black;
		 int maximum = iProcessor->imgdata.color.maximum;

		 int shift = 0;
		 while((maximum << shift) < 32768 && shift < 8)
		 {
			 shift++;
		 }

		 maximum = maximum << shift;
		 black = black << shift;

		 fid = env->GetFieldID(cls, "black", "I");
		 env->SetIntField(obj, fid, black);

		 fid = env->GetFieldID(cls, "maximum", "I");
		 env->SetIntField(obj, fid, maximum);



		 fflush(stdout);

		 int width = S.width;
		 int height = S.height;
		 int raw_width = S.raw_width;
		 //int raw_height = S.raw_height;
		 int x_margin = S.left_margin;
		 int y_margin = S.top_margin;
		 fid = env->GetFieldID(cls, "width", "I");
		 env->SetIntField(obj, fid, width);
		 fid = env->GetFieldID(cls, "height", "I");
		 env->SetIntField(obj, fid, height);


		 jcharArray  jb = 0;

		 libraw_decoder_info_t decoder_info;
		 iProcessor->get_decoder_info(&decoder_info);
		 if(!(decoder_info.decoder_flags & LIBRAW_DECODER_FLATFIELD))
			 {
				 fprintf(stderr, "Only Bayer-pattern RAW files supported, sorry....\n");
			 } else {

				 jb = env->NewCharArray(width * height);
				 // decoder_info.cdesc => RGBG
				 for(int y = 0; y < height; ++y)
				 {
					 ushort * pixels = iProcessor->imgdata.rawdata.raw_image + raw_width * (y + y_margin) + x_margin;
					 for(int i =0; i < width; ++i)
					 {
						 pixels[i] = pixels[i] << shift;
					 }
					 // Shift
					 env->SetCharArrayRegion(jb, y * width, width, (jchar*) pixels);
				 }
			 }


		 // fprintf(stderr, "coucou: %#x\n", (unsigned int)iProcessor->imgdata.image[0]);

		 fflush(stderr);
		 // ;

		 if (iProcessor) delete iProcessor;
		 free(path);
		 env->ReleaseByteArrayElements(jpath, pathNoZT, 0);
		 return jb;
	 } catch(...) {
		 if (iProcessor) delete iProcessor;
		 free(path);
		 env->ReleaseByteArrayElements(jpath, pathNoZT, 0);
		 return 0;
	 }
}
