## Introduction  

Glyco-PAINT is a research method to study interactions of glycans on lectins on live cells. The Glyco-PAINT Application Processing Pipeline is a software solution to process and analyse experimental results.



## Reference
The software and selected research results are described in:  Steuten, K., Bakker, J., Doelman, W.  et al. Glyco-PAINT-APP: Subcellular analysis of immune cell-lectin binding enables correlation of glycan binding parameters to receptor biology.  doi: https://doi.org/10.1101/2025.01.24.634682 [![DOI](https://img.shields.io/badge/bioRxiv-10.1101%2F2025.01.24.634682-blue)](https://doi.org/10.1101/2025.01.24.634682)

The Pipeline, presented here, is a research application to extract information from the GlycoPaint recordings. The pipeline is a collection of Python and R scripts and depends heavily on the Glyco-PAINT method, Fiji and TrackMate.

> Riera, R., Hogervorst, T.P., Doelman, W. et al. Single-molecule imaging of glycan–lectin interactions on cells with Glyco-PAINT. Nat Chem Biol 17, 1281–1288 (2021). https://doi.org/10.1038/s41589-021-00896-2
> 
> Ershov, D., Phan, M.-S., Pylvänäinen, J. W., Rigaud, S. U., Le Blanc, L., Charles-Orszag, A., … Tinevez, J.-Y. (2022).
TrackMate 7: integrating state-of-the-art segmentation algorithms into tracking pipelines. Nature Methods, 19(7),
829–832. doi:10.1038/s41592-022-01507-1





## Earlier version 

The current version of the pipeline is a Java rewrite of the first pipeline written in Python (https://github.com/Leiden-chemical-immunology/Glyco-PAINT). This new version is functionally equivalent, but has many improvements in terms of userability, simplicity and maintainability.



## Software environment 

The software has been tested on a MacBook M3 Max with MacOS 26. 



## System requirements

No special system requirements are identified, other than a minimum memory of 16GB. More memory and a fast processor will reduce runtimes. 



## License

The software is provided as is under the [MIT license](LICENSE).



## Functionality

An overview of the functionality of the pipeline, how to use it and a detailed demo case with sample data it is provided in the [Functional Specifications](doc/Functional Specifations.md).



## Sample image data 

Reference  image data is provided on Zenodo: https://doi.org/10.5281/zenodo.17487086