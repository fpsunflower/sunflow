#include "sunflowBucketToRenderView.h"

DisplayPacket bucketToRenderView::getPacket(){
   DisplayPacket p;
   p.type.sbits8[3] = fgetc( renderPipe );
   p.type.sbits8[2] = fgetc( renderPipe );
   p.type.sbits8[1] = fgetc( renderPipe );
   p.type.sbits8[0] = fgetc( renderPipe );
   for (int i = 0; i < 4; i++) {
       p.data[i].sbits8[3] = fgetc( renderPipe );
       p.data[i].sbits8[2] = fgetc( renderPipe );
       p.data[i].sbits8[1] = fgetc( renderPipe );
       p.data[i].sbits8[0] = fgetc( renderPipe );
   }
   //std::cout << "Recieved packet "<<p.type.sbits32<<" - ["<<p.data[0].sbits32<<" "<<p.data[1].sbits32<<" "<<p.data[2].sbits32<<" "<<p.data[3].sbits32<<"]"<<std::endl;		
   return p;
}

void bucketToRenderView::checkStream() {   
   DisplayPacket p = getPacket();
   switch (p.type.sbits32) {
       case 6: {
		   std::cout << "  * starting to recieve frame "<< p.data[0].sbits32<< std::endl;
           break;
       }
       case 5: {
           renderWidth = p.data[0].sbits32;
           renderHeight = p.data[1].sbits32;
           float g = p.data[2].f;
           std::cout << "  * starting image("<<renderWidth<<" x "<<renderHeight<<"), gamma="<<g<< std::endl;
		   if(renderView.doesRenderEditorExist()){
			   renderView.setCurrentCamera(renderCamera);
			   renderView.startRender(renderWidth, renderHeight, false, true);
		   }
           //window->resizeImage(w, h);
           //window->redraw();
           break;
       }
       case 2: {
           int xl = p.data[0].sbits32;
           int xh = p.data[1].sbits32;
           int yl = p.data[2].sbits32;
           int yh = p.data[3].sbits32;
           int size = (xh - xl + 1) * (yh - yl + 1);
		   RV_PIXEL *data;
		   data = (RV_PIXEL*) malloc(sizeof(RV_PIXEL)*((xh-xl+1)*(yh-yl+1)));
		   
           //std::vector<char> rgba(size, 0);
		   for (int i = 0; i < size; i++){
				data[i].r = fgetc( renderPipe );
				data[i].g =	fgetc( renderPipe );
				data[i].b = fgetc( renderPipe );
				data[i].a = fgetc( renderPipe );
		   }
           //std::cout << "  * getting image tile ("<<xl<<", "<<yl<<") to ("<<xh<<", "<<yh<<")" << std::endl;
		   renderView.updatePixels ( xl, xh, yl,yh,data );
		   renderView.refresh( xl, xh, yl, yh );

		   //}
           //window->updateTile(xl, xh, yl, yh, &rgba[0]);
           //window->redraw();
           break;
       }
       case 4: {
           std::cout << "  * done receiving frame" << std::endl;
           //Fl::remove_idle(checkStream, window);
		   renderView.endRender();
           break;
       }
       case 3: {
           std::cout << "  * connection denied!" << std::endl;
           //Fl::remove_idle(checkStream, window);
           break;
       }
       default: {
           std::cout << "  * unknown packet type" << std::endl;
           break;
       }
   }
} 