package apollo.dataadapter.synteny;

import apollo.datamodel.Link;

interface ResultAnnotPairI {
  boolean isLinked();
  Link createLink();
}
